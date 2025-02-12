package com.traveltime.plugin.solr.query;

import static org.apache.lucene.search.DocIdSetIterator.NO_MORE_DOCS;

import com.traveltime.plugin.solr.cache.CachedData;
import com.traveltime.plugin.solr.util.Util;
import java.io.IOException;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.docvalues.IntDocValues;

@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TravelTimeValueSource<Params extends QueryParams> extends ValueSource {
  private final Params params;
  @EqualsAndHashCode.Exclude private final CachedData cache;

  @Override
  public FunctionValues getValues(Map context, LeafReaderContext readerContext) throws IOException {
    SortedNumericDocValues longs =
        DocValues.getSortedNumeric(readerContext.reader(), params.getField());
    return new IntDocValues(this) {
      @Override
      public int intVal(int doc) throws IOException {
        int time = -1;
        if (longs.advance(doc) != NO_MORE_DOCS) time = cache.get(Util.decode(longs.nextValue()));
        return time;
      }
    };
  }

  @Override
  public String description() {
    return "traveltime";
  }
}
