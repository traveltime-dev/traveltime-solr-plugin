package com.traveltime.plugin.solr.query;

import com.traveltime.plugin.solr.fetcher.Fetcher;
import com.traveltime.plugin.solr.util.Util;
import lombok.val;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.SyntaxError;

public class TravelTimeQueryParser extends QParser {
  private final Fetcher<TravelTimeQueryParameters> fetcher;
  private final String cacheName;
  private final boolean isFilteringDisabled;
  private final String paramPrefix;

  public TravelTimeQueryParser(
      String qstr,
      SolrParams localParams,
      SolrParams params,
      SolrQueryRequest req,
      Fetcher<TravelTimeQueryParameters> fetcher,
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
  public TravelTimeSearchQuery<TravelTimeQueryParameters> parse() throws SyntaxError {
    val paramSource =
        new ParamSource<>(SolrParamsAdapterImpl.INSTANCE, paramPrefix, localParams, params);

    val weightParser = new WeightParser<SyntaxError>();

    float weight = weightParser.parseWeight(paramSource);

    val parameterParser =
        new TravelTimeQueryParametersParser<SolrParams, SyntaxError>(
            Util.fieldValidator(req.getSchema()), Util::toGeoPoint);

    val params = parameterParser.parse(paramSource);
    return new TravelTimeSearchQuery<>(params, weight, fetcher, cacheName, isFilteringDisabled);
  }
}
