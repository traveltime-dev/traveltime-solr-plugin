package com.traveltime.plugin.solr.query.timefilter;

import com.traveltime.plugin.solr.fetcher.Fetcher;
import com.traveltime.plugin.solr.query.ParamSource;
import com.traveltime.plugin.solr.query.SolrParamsAdapterImpl;
import com.traveltime.plugin.solr.query.TravelTimeSearchQuery;
import com.traveltime.plugin.solr.query.WeightParser;
import com.traveltime.plugin.solr.util.Util;
import lombok.val;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.SyntaxError;

public class TimeFilterQueryParser extends QParser {
  private final Fetcher<TimeFilterQueryParameters> fetcher;
  private final String cacheName;
  private final boolean isFilteringDisabled;
  private final String paramPrefix;

  private final WeightParser<SyntaxError> weightParser = new WeightParser<>();

  public TimeFilterQueryParser(
      String qstr,
      SolrParams localParams,
      SolrParams params,
      SolrQueryRequest req,
      Fetcher<TimeFilterQueryParameters> fetcher,
      String cacheName,
      boolean isFilteringDisabled,
      String paramPrefix) {
    super(qstr, localParams, params, req);
    this.fetcher = fetcher;
    this.cacheName = cacheName;
    this.isFilteringDisabled = isFilteringDisabled;
    this.paramPrefix = paramPrefix;
  }

  @Override
  public TravelTimeSearchQuery<TimeFilterQueryParameters> parse() throws SyntaxError {
    val paramSource =
        new ParamSource<>(SolrParamsAdapterImpl.INSTANCE, paramPrefix, localParams, params);

    float weight = weightParser.parseWeight(paramSource);

    val parameterParser =
        new TimeFilterQueryParametersParser<SolrParams, SyntaxError>(
            Util.fieldValidator(req.getSchema()));

    val params = parameterParser.parse(paramSource);
    return new TravelTimeSearchQuery<>(params, weight, fetcher, cacheName, isFilteringDisabled);
  }
}
