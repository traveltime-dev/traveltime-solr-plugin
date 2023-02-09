package com.traveltime.plugin.solr.query;


import com.traveltime.plugin.solr.ProtoFetcher;
import com.traveltime.plugin.solr.cache.RequestCache;
import com.traveltime.plugin.solr.cache.TravelTimes;
import com.traveltime.plugin.solr.cache.UnprotectedTimes;
import com.traveltime.plugin.solr.util.Util;
import com.traveltime.sdk.dto.common.Coordinates;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import lombok.val;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.util.BitSetIterator;
import org.apache.lucene.util.FixedBitSet;
import org.apache.solr.search.DelegatingCollector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TraveltimeDelegatingCollector extends DelegatingCollector {
   private final LeafReaderContext[] contexts;
   private final int[] contextBaseStart;
   private final int[] contextBaseEnd;

   private final int maxDoc;
   private final Int2FloatOpenHashMap score;
   private final FixedBitSet collectedGlobalDocs;
   private final TraveltimeQueryParameters params;
   private final float scoreWeight;
   private final Int2ObjectOpenHashMap<Coordinates> globalDoc2Coords;
   private final ProtoFetcher fetcher;
   private final RequestCache cache;
   private final boolean isFilteringDisabled;

   private Object2IntOpenHashMap<Coordinates> pointToTime;
   private SortedNumericDocValues coords;


   public TraveltimeDelegatingCollector(int maxDoc, int segments, TraveltimeQueryParameters params, float scoreWeight, ProtoFetcher fetcher, RequestCache cache, boolean isFilteringDisabled) {
      this.maxDoc = maxDoc;
      this.contexts = new LeafReaderContext[segments];
      this.contextBaseStart = new int[segments];
      this.contextBaseEnd = new int[segments];
      this.score = new Int2FloatOpenHashMap(maxDoc);
      this.globalDoc2Coords = new Int2ObjectOpenHashMap<>(maxDoc);
      this.collectedGlobalDocs = new FixedBitSet(maxDoc);
      this.params = params;
      this.scoreWeight = scoreWeight;
      this.fetcher = fetcher;
      this.cache = cache;
      this.isFilteringDisabled = isFilteringDisabled;
   }

   @Override
   protected void doSetNextReader(LeafReaderContext context) throws IOException {
      contexts[context.ord] = context;

      contextBaseStart[context.ord] = context.docBase;
      if (context.ord != 0) contextBaseEnd[context.ord - 1] = context.docBase - 1;
      if (context.ord == contexts.length - 1) contextBaseEnd[context.ord] = maxDoc;

      docBase = context.docBase;
      coords = DocValues.getSortedNumeric(context.reader(), params.getField());
   }

   @Override
   public void setScorer(Scorer scorer) throws IOException {
      super.setScorer(scorer);
   }

   @Override
   public void collect(int contextDoc) throws IOException {
      if (coords.advanceExact(contextDoc)) {
         int globalDoc = this.docBase + contextDoc;
         collectedGlobalDocs.set(globalDoc);
         score.put(globalDoc, scorer.score());
         globalDoc2Coords.put(globalDoc, Util.decode(coords.nextValue()));
      }
   }

   private Object2IntOpenHashMap<Coordinates> computePointToTime(ObjectCollection<Coordinates> coords) {
      TravelTimes cachedResults;
      if (cache != null) {
         cachedResults = cache.getOrFresh(params);
      } else {
         cachedResults = new UnprotectedTimes();
      }

      val nonCachedSet = cachedResults.nonCached(params.getLimit(), coords);

      ArrayList<Coordinates> destinations = new ArrayList<>(nonCachedSet);

      List<Integer> times;
      if (destinations.size() == 0) {
         times = new ArrayList<>();
      } else {
         times = fetcher.getTimes(
               params.getOrigin(),
               destinations,
               params.getLimit(),
               params.getMode(),
               params.getCountry()
         );
      }

      cachedResults.putAll(params.getLimit(), destinations, times);

      return cachedResults.mapToTimes(params.getLimit(), coords);
   }

   @Override
   public void finish() throws IOException {
      if (contexts.length == 0)
         return;

      pointToTime = computePointToTime(globalDoc2Coords.values());

      val collectedDocs = new BitSetIterator(collectedGlobalDocs, 0L);
      val forwardingScorer = new ForwardingScorer(collectedDocs);

      int currentContextIndex = 0;
      while (collectedDocs.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
         int globalDoc = collectedDocs.docID();

         while (globalDoc > contextBaseEnd[currentContextIndex]) {
            currentContextIndex++;
         }

         if (isFilteringDisabled || pointToTime.containsKey(globalDoc2Coords.get(globalDoc))) {
            int contextDoc = globalDoc - contextBaseStart[currentContextIndex];
            leafDelegate = delegate.getLeafCollector(contexts[currentContextIndex]);
            leafDelegate.setScorer(forwardingScorer);
            leafDelegate.collect(contextDoc);
         }
      }

      if (delegate instanceof DelegatingCollector) {
         ((DelegatingCollector) delegate).finish();
      }
   }

   private class ForwardingScorer extends Scorer {

      private final DocIdSetIterator backingIterator;

      private ForwardingScorer(DocIdSetIterator backingIterator) {
         super(null);
         this.backingIterator = backingIterator;
      }

      @Override
      public int docID() {
         return backingIterator.docID();
      }

      @Override
      public float score() {
         int limit = params.getLimit();
         int time = pointToTime.getOrDefault(globalDoc2Coords.get(docID()), params.getLimit() + 1);
         float ttScore = (float) (limit - time + 1) / (limit + 1);
         return (1f - scoreWeight) * score.get(docID()) + scoreWeight * ttScore;
      }

      @Override
      public DocIdSetIterator iterator() {
         throw new UnsupportedOperationException();
      }
   }
}
