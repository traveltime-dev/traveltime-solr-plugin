package com.traveltime.plugin.solr;

import static com.traveltime.plugin.solr.query.ParamSource.PARAM_PREFIX;

import com.traveltime.plugin.solr.cache.RequestCache;
import com.traveltime.plugin.solr.fetcher.ProtoFetcherSingleton;
import com.traveltime.plugin.solr.query.TravelTimeQueryParser;
import java.net.URI;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;

public class TravelTimeQParserPlugin extends QParserPlugin {
  private String cacheName = RequestCache.NAME;
  private boolean isFilteringDisabled = false;
  private String paramPrefix = PARAM_PREFIX;

  private URI uri = null;
  private String appId = null;
  private String apiKey = null;

  @Override
  public void init(NamedList args) {
    Object cache = args.get("cache");
    if (cache != null) cacheName = cache.toString();

    Object filteringDisabled = args.get("filtering_disabled");
    if (filteringDisabled != null)
      this.isFilteringDisabled = Boolean.parseBoolean(filteringDisabled.toString());

    Object prefix = args.get("prefix");
    if (prefix != null) paramPrefix = prefix.toString();

    Object uriVal = args.get("api_uri");
    if (uriVal != null) uri = URI.create(uriVal.toString());

    appId = args.get("app_id").toString();
    apiKey = args.get("api_key").toString();
  }

  @Override
  public QParser createParser(
      String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
    Thread.currentThread()
        .setContextClassLoader(req.getCore().getResourceLoader().getClassLoader());
    ProtoFetcherSingleton.INSTANCE.init(uri, appId, apiKey);

    return new TravelTimeQueryParser(
        qstr,
        localParams,
        params,
        req,
        ProtoFetcherSingleton.INSTANCE.getFetcher(),
        cacheName,
        isFilteringDisabled,
        paramPrefix);
  }
}
