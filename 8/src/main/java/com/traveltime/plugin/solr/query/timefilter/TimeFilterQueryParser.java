package com.traveltime.plugin.solr.query.timefilter;

import com.traveltime.plugin.solr.fetcher.Fetcher;
import com.traveltime.plugin.solr.query.ParamSource;
import com.traveltime.plugin.solr.query.TraveltimeSearchQuery;
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

   public TimeFilterQueryParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req, Fetcher<TimeFilterQueryParameters> fetcher, String cacheName, boolean isFilteringDisabled) {
      super(qstr, localParams, params, req);
      this.fetcher = fetcher;
      this.cacheName = cacheName;
      this.isFilteringDisabled = isFilteringDisabled;
   }

   @Override
   public TraveltimeSearchQuery<TimeFilterQueryParameters> parse() throws SyntaxError {
      ParamSource paramSource = new ParamSource(localParams, params);
      float weight;
      try {
         weight = Float.parseFloat(paramSource.getParam(WEIGHT));
      } catch (SyntaxError ignored) {
         weight = 0f;
      } catch (NumberFormatException e) {
         throw new SyntaxError("Couldn't parse traveltime weight as a float");
      }
      if (weight < 0 || weight > 1) {
         throw new SyntaxError("Traveltime weight must be between 0 and 1");
      }

      val params = TimeFilterQueryParameters.parse(req.getSchema(), paramSource);
      return new TraveltimeSearchQuery<>(params, weight, fetcher, cacheName, isFilteringDisabled);
   }

}
