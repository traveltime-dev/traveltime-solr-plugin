package com.traveltime.plugin.solr.query;

import com.traveltime.plugin.solr.cache.RequestCache;
import lombok.val;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.FunctionQParser;
import org.apache.solr.search.SyntaxError;
import org.apache.solr.search.ValueSourceParser;

import static com.traveltime.plugin.solr.TraveltimeQParserPlugin.PARAM_PREFIX;

public class TraveltimeValueSourceParser extends ValueSourceParser {
   private String cacheName = RequestCache.NAME;

   @Override
   public void init(NamedList args) {
      super.init(args);
      Object cache = args.get("cache");
      if (cache != null) cacheName = cache.toString();
   }

   private String getParam(SolrParams params, String name) throws SyntaxError {
      String param = params.get(PARAM_PREFIX + name);
      if (param != null) return param;
      param = params.get(name);
      if (param != null) return param;
      throw new SyntaxError("missing " + name + " parameter for TravelTime value source");
   }


   @Override
   public ValueSource parse(FunctionQParser fp) throws SyntaxError {
      SolrQueryRequest req = fp.getReq();
      RequestCache cache = (RequestCache) req.getSearcher().getCache(cacheName);
      if (cache == null) {
         throw new SolrException(
               SolrException.ErrorCode.BAD_REQUEST,
               "No request cache configured."
         );
      }

      SolrParams params = fp.getParams();


      val queryParameters = TraveltimeQueryParameters.fromStrings(
            req.getSchema(),
            getParam(params, TraveltimeQueryParameters.FIELD),
            getParam(params, TraveltimeQueryParameters.ORIGIN),
            getParam(params, TraveltimeQueryParameters.LIMIT),
            getParam(params, TraveltimeQueryParameters.MODE),
            getParam(params, TraveltimeQueryParameters.COUNTRY)
      );

      return new TraveltimeValueSource(queryParameters, cache.getOrFresh(queryParameters));
   }
}
