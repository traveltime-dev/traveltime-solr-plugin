package com.traveltime.plugin.solr.query.timefilter;

import com.traveltime.plugin.solr.cache.TravelTimes;
import com.traveltime.plugin.solr.util.Util;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.docvalues.IntDocValues;

import java.io.IOException;
import java.util.Map;

import static org.apache.lucene.search.DocIdSetIterator.NO_MORE_DOCS;

@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TimeFilterValueSource extends ValueSource {
   private final TimeFilterQueryParameters params;
   @EqualsAndHashCode.Exclude
   private final TravelTimes cache;

   @Override
   public FunctionValues getValues(Map context, LeafReaderContext readerContext) throws IOException {
      SortedNumericDocValues longs = DocValues.getSortedNumeric(readerContext.reader(), params.getField());
      return new IntDocValues(this) {
         @Override
         public int intVal(int doc) throws IOException {
            int time = -1;
            if (longs.advance(doc) != NO_MORE_DOCS)
               time = cache.get(Util.decode(longs.nextValue()));
            return time;
         }
      };
   }

   @Override
   public String description() {
      return "traveltime";
   }
}
