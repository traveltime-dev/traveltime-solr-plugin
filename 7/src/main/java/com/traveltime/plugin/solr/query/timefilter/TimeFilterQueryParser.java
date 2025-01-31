package com.traveltime.plugin.solr.query.timefilter;

import com.traveltime.plugin.solr.fetcher.Fetcher;
import com.traveltime.plugin.solr.query.ParamSource;
import com.traveltime.plugin.solr.query.SolrParamsAdapterImpl;
import com.traveltime.plugin.solr.query.TravelTimeSearchQuery;
import com.traveltime.plugin.solr.util.Util;
import lombok.val;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.SyntaxError;

public class TimeFilterQueryParser extends QParser {
  private static final String WEIGHT = "weight";

  private final Fetcher<TimeFilterQueryParameters> fetcher;
  private final String cacheName;
  private final boolean isFilteringDisabled;
  private final String paramPrefix;

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
    float weight;
    try {
      weight = Float.parseFloat(paramSource.getParam(WEIGHT));
    } catch (SyntaxError ignored) {
      weight = 0f;
    } catch (NumberFormatException e) {
      throw new SyntaxError("Couldn't parse traveltime weight as a float");
    }
    if (weight < 0 || weight > 1) {
      throw new SyntaxError("TravelTime weight must be between 0 and 1");
    }

    val parameterParser =
        new TimeFilterQueryParametersParser<>(
            SolrParamsAdapterImpl.INSTANCE, Util.fieldValidator(req.getSchema()));

    val params = parameterParser.parse(paramSource);
    return new TravelTimeSearchQuery<>(params, weight, fetcher, cacheName, isFilteringDisabled);
  }
}
