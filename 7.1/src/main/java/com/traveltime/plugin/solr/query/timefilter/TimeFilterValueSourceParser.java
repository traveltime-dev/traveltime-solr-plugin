package com.traveltime.plugin.solr.query.timefilter;

import static com.traveltime.plugin.solr.query.ParamSource.PARAM_PREFIX;

import com.traveltime.plugin.solr.cache.RequestCache;
import com.traveltime.plugin.solr.query.ParamSource;
import com.traveltime.plugin.solr.query.SolrParamsAdapterImpl;
import com.traveltime.plugin.solr.query.TravelTimeValueSource;
import com.traveltime.plugin.solr.util.Util;
import lombok.val;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.FunctionQParser;
import org.apache.solr.search.SyntaxError;
import org.apache.solr.search.ValueSourceParser;

public class TimeFilterValueSourceParser extends ValueSourceParser {
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
    RequestCache<TimeFilterQueryParameters> cache =
        (RequestCache<TimeFilterQueryParameters>) req.getSearcher().getCache(cacheName);
    if (cache == null) {
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "No request cache configured.");
    }

    val timeFilterQueryParametersParser =
        new TimeFilterQueryParametersParser<SolrParams, SyntaxError>(
            Util.fieldValidator(req.getSchema()));

    TimeFilterQueryParameters queryParams =
        timeFilterQueryParametersParser.parse(
            new ParamSource<>(SolrParamsAdapterImpl.INSTANCE, paramPrefix, fp.getParams()));
    return new TravelTimeValueSource<>(queryParams, cache.getOrFresh(queryParams).getTimes());
  }
}
