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
 * Parts of this code are based on LatLonPointDistanceFeatureQuery.
 * DistanceScorer and LongDistanceFeatureQuery.DistanceScorer from the
 * Apache Lucene project.
 */
package com.traveltime.plugin.solr.query;

import com.traveltime.plugin.solr.cache.RequestCache;
import com.traveltime.plugin.solr.cache.TravelTimes;
import com.traveltime.plugin.solr.fetcher.Fetcher;
import com.traveltime.sdk.dto.common.Coordinates;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.apache.lucene.geo.GeoEncodingUtils;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.solr.search.DelegatingCollector;
import org.apache.solr.search.ExtendedQueryBase;
import org.apache.solr.search.PostFilter;
import org.apache.solr.search.SolrIndexSearcher;

import java.io.IOException;
import java.util.Set;

@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TraveltimeSearchQuery<Params extends QueryParams> extends ExtendedQueryBase implements PostFilter {
   private final Params params;
   private final float weight;
   private final Fetcher<Params> fetcher;
   private final String cacheName;
   private final boolean isFilteringDisabled;

   @Override
   public String toString(String field) {
      return String.format("TraveltimeSearchQuery(params = %s)", params);
   }

   @Override
   public DelegatingCollector getFilterCollector(IndexSearcher indexSearcher) {
      SolrIndexSearcher searcher = (SolrIndexSearcher) indexSearcher;
      RequestCache<Params> cache = (RequestCache<Params>) searcher.getCache(cacheName);
      int maxDoc = searcher.maxDoc();
      int leafCount = searcher.getTopReaderContext().leaves().size();
      return new TraveltimeDelegatingCollector<>(maxDoc, leafCount, params, weight, fetcher, cache, isFilteringDisabled);
   }

   @Override
   public boolean getCache() {
      return false;
   }

   @Override
   public void setCache(boolean cache) {
   }

   @Override
   public int getCost() {
      return 100;
   }

   @Override
   public void setCost(int cost) {
   }

   @Override
   public boolean getCacheSep() {
      return false;
   }

   @Override
   public void setCacheSep(boolean cacheSep) {
   }

   @Override
   public Weight createWeight(IndexSearcher indexSearcher, ScoreMode scoreMode, float boost) {
      SolrIndexSearcher searcher = (SolrIndexSearcher)indexSearcher;
      RequestCache<Params> cache = (RequestCache<Params>) searcher.getCache(cacheName);
      TravelTimes travelTimes = cache.get(params);
      return new Weight(this) {

         private final int limit = params.getTravelTime();
         private final double limitAsDouble = limit;

         @Override
         public boolean isCacheable(LeafReaderContext ctx) {
            return false;
         }

         @Override
         public void extractTerms(Set<Term> terms) {}

         @Override
         public Explanation explain(LeafReaderContext context, int doc) throws IOException {
            SortedNumericDocValues multiDocValues = DocValues.getSortedNumeric(context.reader(), params.getField());
            if (!multiDocValues.advanceExact(doc)) {
               return Explanation.noMatch("Document " + doc + " doesn't have a value for field " + params.getField());
            }
            long encodedDocumentCoordinate = selectValue(multiDocValues);
            int documentLatitudeBits = (int)(encodedDocumentCoordinate >> 32);
            int documentLongitudeBits = (int) encodedDocumentCoordinate;
            double documentLat = GeoEncodingUtils.decodeLatitude(documentLatitudeBits);
            double documentLon = GeoEncodingUtils.decodeLongitude(documentLongitudeBits);

            int travelTime = travelTimes == null ? -1 : travelTimes.get(new Coordinates(documentLat, documentLon));

            // score = if (travel time is unknown) 0.0 else boost * limit / (limit + travel time)
            //
            // This results in scores 0.0 and in the range [0.5, 1.0]. Short
            // travel times will get a relatively higher score. This is based
            // on the assumption that people do not have a linear preference
            // for travel times. Travel times at or near the limit will get a
            // score significantly better than unavailable travel times:
            // >= 0.5 versus 0.0.
            float score = travelTime == -1 ? 0.0f : (float) (boost * (limitAsDouble / (limitAsDouble + travelTime)));

            Coordinates queryOrigin = params.getOrigin();
            return Explanation.match(score, params.getTransportMode() + " score, computed as, when present, boost * limit / (limit + travelTime), otherwise 0.0, from:",
                    Explanation.match(boost, "weight"),
                    Explanation.match(limit, "maximum travel time"),
                    Explanation.match(queryOrigin.getLat(), "query lat"),
                    Explanation.match(queryOrigin.getLng(), "query lon"),
                    Explanation.match(documentLat, "document lat"),
                    Explanation.match(documentLon, "document lon"),
                    Explanation.match(travelTime, "travel time"));
         }

         private long selectValue(SortedNumericDocValues multiDocValues) throws IOException {
            int count = multiDocValues.docValueCount();
            long value = multiDocValues.nextValue();
            if (count > 1) {
               throw new IllegalStateException("Multi-valued field " + params.getField() + " is not supported");
            }
            return value;
         }

         private NumericDocValues selectValues(SortedNumericDocValues multiDocValues) {
            final NumericDocValues singleton = DocValues.unwrapSingleton(multiDocValues);
            if (singleton != null) {
               return singleton;
            }
            return  new NumericDocValues() {

               long value;

               @Override
               public long longValue() {
                  return value;
               }

               @Override
               public boolean advanceExact(int target) throws IOException {
                  if (multiDocValues.advanceExact(target)) {
                     value = selectValue(multiDocValues);
                     return true;
                  } else {
                     return false;
                  }
               }

               @Override
               public int docID() {
                  return multiDocValues.docID();
               }

               @Override
               public int nextDoc() throws IOException {
                  return multiDocValues.nextDoc();
               }

               @Override
               public int advance(int target) throws IOException {
                  return multiDocValues.advance(target);
               }

               @Override
               public long cost() {
                  return multiDocValues.cost();
               }

            };
         }

         @Override
         public ScorerSupplier scorerSupplier(LeafReaderContext context) throws IOException {
            PointValues pointValues = context.reader().getPointValues(params.getField());
            if (pointValues == null) {
               // No data on this segment
               return null;
            }
            final SortedNumericDocValues multiDocValues = DocValues.getSortedNumeric(context.reader(), params.getField());
            final NumericDocValues docValues = selectValues(multiDocValues);

            final Weight weight = this;
            return new ScorerSupplier() {

               @Override
               public Scorer get(long leadCost) {
                  return new TravelTimeScorer(params.getTravelTime(), travelTimes, weight, context.reader().maxDoc(), leadCost, boost, pointValues, docValues);
               }

               @Override
               public long cost() {
                  return docValues.cost();
               }
            };
         }

         @Override
         public Scorer scorer(LeafReaderContext context) throws IOException {
            ScorerSupplier scorerSupplier = scorerSupplier(context);
            if (scorerSupplier == null) {
               return null;
            }
            return scorerSupplier.get(Long.MAX_VALUE);
         }

      };
   }
}
