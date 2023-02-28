package com.traveltime.plugin.solr;

import com.traveltime.plugin.solr.cache.RequestCache;
import com.traveltime.plugin.solr.fetcher.JsonFetcherSingleton;
import com.traveltime.plugin.solr.query.timefilter.TimeFilterQueryParser;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;

import java.net.URI;
import java.util.Optional;

import static com.traveltime.plugin.solr.query.ParamSource.PARAM_PREFIX;

public class TimeFilterQParserPlugin extends QParserPlugin {
   private String cacheName = RequestCache.NAME;
   private boolean isFilteringDisabled = false;
   private String paramPrefix = PARAM_PREFIX;

   private static final Integer DEFAULT_LOCATION_SIZE_LIMIT = 2000;

   @Override
   public void init(NamedList args) {
      Object cache = args.get("cache");
      if (cache != null) cacheName = cache.toString();

      Object filteringDisabled = args.get("filtering_disabled");
      if (filteringDisabled != null) this.isFilteringDisabled = Boolean.parseBoolean(filteringDisabled.toString());
      
      Object prefix = args.get("prefix");
      if (prefix != null) paramPrefix = prefix.toString();

      Object uriVal = args.get("api_uri");
      URI uri = null;
      if (uriVal != null) uri = URI.create(uriVal.toString());

      String appId = args.get("app_id").toString();
      String apiKey = args.get("api_key").toString();
      int locationLimit =
            Optional.ofNullable(args.get("location_limit"))
                    .map(x -> Integer.parseInt(x.toString()))
                    .orElse(DEFAULT_LOCATION_SIZE_LIMIT);

      JsonFetcherSingleton.INSTANCE.init(uri, appId, apiKey, locationLimit);
   }

   @Override
   public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
      return new TimeFilterQueryParser(qstr,
                                       localParams,
                                       params,
                                       req,
                                       JsonFetcherSingleton.INSTANCE.getFetcher(),
                                       cacheName,
                                       isFilteringDisabled
                                       paramPrefix
      );
   }

}
