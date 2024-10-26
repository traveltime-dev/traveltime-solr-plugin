package com.traveltime.plugin.solr.query;

import com.traveltime.plugin.solr.cache.RequestCache;
import lombok.val;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.FunctionQParser;
import org.apache.solr.search.SyntaxError;
import org.apache.solr.search.ValueSourceParser;

import static com.traveltime.plugin.solr.query.ParamSource.PARAM_PREFIX;

public class TravelTimeValueSourceParser extends ValueSourceParser {
   private String cacheName = RequestCache.NAME;

   private String paramPrefix = PARAM_PREFIX;

   @Override
   public void init(NamedList args) {
      super.init(args);
      Object cache = args.get("cache");
      if (cache != null) cacheName = cache.toString();

      Object prefix = args.get("prefix");
      if (prefix != null) paramPrefix = prefix.toString();
   }

   @Override
   public ValueSource parse(FunctionQParser fp) throws SyntaxError {
      SolrQueryRequest req = fp.getReq();
      RequestCache<TravelTimeQueryParameters> cache = (RequestCache<TravelTimeQueryParameters>) req.getSearcher()
                                                                                                   .getCache(cacheName);
      if (cache == null) {
         throw new SolrException(
               SolrException.ErrorCode.BAD_REQUEST,
               "No request cache configured."
         );
      }

      val queryParameters = TravelTimeQueryParameters.parse(
            req.getSchema(),
            new ParamSource(paramPrefix, fp.getParams())
      );
      return new TravelTimeValueSource<>(queryParameters, cache.getOrFresh(queryParameters));
   }
}
