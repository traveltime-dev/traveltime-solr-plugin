package com.traveltime.plugin.solr.query;

import com.traveltime.plugin.solr.fetcher.Fetcher;
import lombok.val;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.SyntaxError;

public class TraveltimeQueryParser extends QParser {
   private static final String WEIGHT = "weight";

   private final Fetcher<TraveltimeQueryParameters> fetcher;
   private final String cacheName;
   private final boolean isFilteringDisabled;
   private final String paramPrefix;

   public TraveltimeQueryParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req, Fetcher<TraveltimeQueryParameters> fetcher, String cacheName, boolean isFilteringDisabled, String paramPrefix) {
      super(qstr, localParams, params, req);
      this.fetcher = fetcher;
      this.cacheName = cacheName;
      this.isFilteringDisabled = isFilteringDisabled;
      this.paramPrefix = paramPrefix;
   }

   @Override
   public TraveltimeSearchQuery<TraveltimeQueryParameters> parse() throws SyntaxError {
      ParamSource paramSource = new ParamSource(paramPrefix, localParams, params);
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

      val params = TraveltimeQueryParameters.parse(req.getSchema(), paramSource);
      return new TraveltimeSearchQuery<>(params, weight, fetcher, cacheName, isFilteringDisabled);
   }

}
