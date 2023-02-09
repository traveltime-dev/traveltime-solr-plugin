/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * This code is based on LatLonPointDistanceFeatureQuery.DistanceScorer and
 * LongDistanceFeatureQuery.DistanceScorer from the Apache Lucene project.
 */
package com.traveltime.plugin.solr.query;

import com.traveltime.plugin.solr.cache.TravelTimes;
import com.traveltime.sdk.dto.common.Coordinates;
import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.geo.GeoEncodingUtils;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.PointValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.DocIdSetBuilder;
import org.apache.lucene.util.NumericUtils;

import java.io.IOException;

class TravelTimeScorer extends Scorer {

    private final int maxDoc;
    private DocIdSetIterator it;
    private int doc = -1;
    private final long leadCost;
    private final float boost;
    private final PointValues pointValues;
    private final NumericDocValues docValues;
    private final int limit;
    // Stored as a double so conversion from int to double does not have to be
    // done every time the score is calculated.
    private final double limitAsDouble;
    private final TravelTimes travelTimes;
    private int maxTravelTime;

    protected TravelTimeScorer(int limit, TravelTimes travelTimes, Weight weight, int maxDoc, long leadCost, float boost,
                               PointValues pointValues, NumericDocValues docValues) {
        super(weight);
        this.limit = limit;
        this.limitAsDouble = limit;
        this.travelTimes = travelTimes;
        this.maxTravelTime = limit;
        this.maxDoc = maxDoc;
        this.leadCost = leadCost;
        this.boost = boost;
        this.pointValues = pointValues;
        this.docValues = docValues;
        // initially use doc values in order to iterate all documents that have
        // a value for this field
        this.it = docValues;
    }

    @Override
    public int docID() {
        return doc;
    }

    private float score(int travelTime) {
        // score = if (travel time is unknown) 0.0 else boost * limit / (limit + travel time)
        //
        // This results in scores 0.0 and in the range [0.5, 1.0]. Short
        // travel times will get a relatively higher score. This is based
        // on the assumption that people do not have a linear preference
        // for travel times. Travel times at or near the limit will get a
        // score significantly better than unavailable travel times:
        // >= 0.5 versus 0.0.
        return travelTime == -1.0 ? 0.0f : (float) (boost * (limitAsDouble / (limitAsDouble + travelTime)));
    }

    /**
     * Inverting the score computation is very hard due to all potential
     * rounding errors, so we binary search the maximum travel time. The
     * limit is set to 1 second.
     */
    private int computeMaxTravelTime(float minScore, int previousMaxTravelTime) {
        assert score(0) >= minScore;
        if (score(previousMaxTravelTime) >= minScore) {
            // minScore did not decrease enough to require an update to the max travel time
            return previousMaxTravelTime;
        }
        assert score(previousMaxTravelTime) < minScore;
        int min = 0, max = previousMaxTravelTime;
        // invariant: score(min) >= minScore && score(max) < minScore
        while (max - min > 1) {
            int mid = (min + max) / 2;
            float score = score(mid);
            if (score >= minScore) {
                min = mid;
            } else {
                max = mid;
            }
        }
        assert score(min) >= minScore;
        assert min == limit || score(min + 1) < minScore;
        return min;
    }

    @Override
    public float score() throws IOException {
        long encodedDocumentCoordinate = docValues.longValue();
        int documentLatitudeBits = (int)(encodedDocumentCoordinate >> 32);
        int documentLongitudeBits = (int) encodedDocumentCoordinate;
        double documentLat = GeoEncodingUtils.decodeLatitude(documentLatitudeBits);
        double documentLon = GeoEncodingUtils.decodeLongitude(documentLongitudeBits);

        int travelTime = travelTimes == null ? -1 : travelTimes.get(new Coordinates(documentLat, documentLon));

        return docValues.advanceExact(docID()) ? score(travelTime) : 0;
    }

    @Override
    public DocIdSetIterator iterator() {
        // add indirection so that if 'it' is updated then it will
        // be taken into account
        return new DocIdSetIterator() {

            @Override
            public int nextDoc() throws IOException {
                return doc = it.nextDoc();
            }

            @Override
            public int docID() {
                return doc;
            }

            @Override
            public long cost() {
                return it.cost();
            }

            @Override
            public int advance(int target) throws IOException {
                return doc = it.advance(target);
            }
        };
    }

    @Override
    public float getMaxScore(int upTo) {
        return boost;
    }

    private int setMinCompetitiveScoreCounter = 0;


    @Override
    public void setMinCompetitiveScore(float minScore) throws IOException {
        if (minScore > boost) {
            it = DocIdSetIterator.empty();
            return;
        }

        setMinCompetitiveScoreCounter++;
        // We sample the calls to this method as it is expensive to recalculate the iterator.
        if (setMinCompetitiveScoreCounter > 256 && (setMinCompetitiveScoreCounter & 0x1f) != 0x1f) {
            return;
        }

        int previousMaxTravelTime = maxTravelTime;
        maxTravelTime = computeMaxTravelTime(minScore, maxTravelTime);
        if (maxTravelTime == previousMaxTravelTime) {
            // nothing to update
            return;
        }

        DocIdSetBuilder result = new DocIdSetBuilder(maxDoc);
        final int doc = docID();
        PointValues.IntersectVisitor visitor = new PointValues.IntersectVisitor() {

            DocIdSetBuilder.BulkAdder adder;

            @Override
            public void grow(int count) {
                adder = result.grow(count);
            }

            @Override
            public void visit(int docID) {
                if (docID <= doc) {
                    // Already visited or skipped
                    return;
                }
                adder.add(docID);
            }

            @Override
            public void visit(int docID, byte[] packedValue) {
                if (docID <= doc) {
                    // Already visited or skipped
                    return;
                }

                int documentTravelTime = getTravelTimeForCoordinate(packedValue);

                if (documentTravelTime == -1 || documentTravelTime > maxTravelTime) {
                    //Document travel time is longer than maximum travel time
                    return;
                }
                adder.add(docID);
            }

            @Override
            public PointValues.Relation compare(byte[] rangeMinPackedValue, byte[] rangeMaxPackedValue) {
                int rangeMinTravelTime = getTravelTimeForCoordinate(rangeMinPackedValue);
                int rangeMaxTravelTime = getTravelTimeForCoordinate(rangeMaxPackedValue);

                if (rangeMinTravelTime > maxTravelTime || rangeMaxTravelTime < 0) {
                    return PointValues.Relation.CELL_OUTSIDE_QUERY;
                }
                if (rangeMinTravelTime < 0 || rangeMaxTravelTime > maxTravelTime) {
                    return PointValues.Relation.CELL_CROSSES_QUERY;
                }
                return PointValues.Relation.CELL_INSIDE_QUERY;
            }

            private int getTravelTimeForCoordinate(byte[] packedValue) {
                int rangeMaxLatitudeBits = NumericUtils.sortableBytesToInt(packedValue, 0);
                int rangeMaxLongitudeBits = NumericUtils.sortableBytesToInt(packedValue, LatLonPoint.BYTES);
                double rangeMaxLat = GeoEncodingUtils.decodeLatitude(rangeMaxLatitudeBits);
                double rangeMaxLon = GeoEncodingUtils.decodeLongitude(rangeMaxLongitudeBits);
                return travelTimes == null ? -1 : travelTimes.get(new Coordinates(rangeMaxLat, rangeMaxLon));
            }
        };

        final long currentQueryCost = Math.min(leadCost, it.cost());
        final long threshold = currentQueryCost >>> 3;
        long estimatedNumberOfMatches = pointValues.estimatePointCount(visitor); // runs in O(log(numPoints))
        // TODO: what is the right factor compared to the current disi? Is 8 optimal?
        if (estimatedNumberOfMatches >= threshold) {
            // the new range is not selective enough to be worth materializing
            return;
        }
        pointValues.intersect(visitor);
        it = result.build().iterator();
    }

}
