package com.traveltime.plugin.solr.query;

import com.traveltime.plugin.solr.fetcher.Fetcher;
import com.traveltime.plugin.solr.fetcher.ProtoFetcher;
import lombok.val;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.SyntaxError;

public class TraveltimeQueryParser extends QParser {
   private static final String WEIGHT = "weight";

   private final Fetcher<TraveltimeQueryParameters> fetcher;
   private final String cacheName;

   public TraveltimeQueryParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req, ProtoFetcher fetcher, String cacheName) {
      super(qstr, localParams, params, req);
      this.fetcher = fetcher;
      this.cacheName = cacheName;
   }

   @Override
   public TraveltimeSearchQuery<TraveltimeQueryParameters> parse() throws SyntaxError {
      ParamSource paramSource = new ParamSource(localParams, params);
      float weight;
      try {
         weight = Float.parseFloat(paramSource.getParam(WEIGHT));
      } catch (SyntaxError ignored) {
         weight = 0f;
      } catch (NumberFormatException e) {
         throw new SyntaxError("Couldn't parse traveltime weight as a float");
      }
      if(weight < 0 || weight > 1) {
         throw new SyntaxError("Traveltime weight must be between 0 and 1");
      }

      val params = TraveltimeQueryParameters.fromStrings(
          req.getSchema(),
          paramSource.getParam(TraveltimeQueryParameters.FIELD),
          paramSource.getParam(TraveltimeQueryParameters.ORIGIN),
          paramSource.getParam(TraveltimeQueryParameters.LIMIT),
          paramSource.getParam(TraveltimeQueryParameters.MODE),
          paramSource.getParam(TraveltimeQueryParameters.COUNTRY)
      );
      return new TraveltimeSearchQuery<>(params, weight, fetcher, cacheName);
   }

}
