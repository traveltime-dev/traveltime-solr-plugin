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
import org.apache.lucene.geo.GeoEncodingUtils;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

import java.io.IOException;

class TravelTimeScorer extends Scorer {

   private final DocIdSetIterator it;
   private int doc = -1;
   private final float boost;
   private final NumericDocValues docValues;
   // Stored as a double so conversion from int to double does not have to be
   // done every time the score is calculated.
   private final double limitAsDouble;
   private final TravelTimes travelTimes;

   protected TravelTimeScorer(int limit, TravelTimes travelTimes, Weight weight, float boost, NumericDocValues docValues) {
      super(weight);
      this.limitAsDouble = limit;
      this.travelTimes = travelTimes;
      this.boost = boost;
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

   @Override
   public float score() throws IOException {
      long encodedDocumentCoordinate = docValues.longValue();
      int documentLatitudeBits = (int) (encodedDocumentCoordinate >> 32);
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
}
