package com.traveltime.plugin.solr.query;

import com.traveltime.plugin.solr.fetcher.Fetcher;
import com.traveltime.plugin.solr.fetcher.ProtoFetcher;
import lombok.val;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.SyntaxError;

import static com.traveltime.plugin.solr.util.Util.PARAM_PREFIX;

public class TraveltimeQueryParser extends QParser {
   private static final String WEIGHT = "weight";

   private final Fetcher<TraveltimeQueryParameters> fetcher;
   private final String cacheName;

   public TraveltimeQueryParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req, ProtoFetcher fetcher, String cacheName) {
      super(qstr, localParams, params, req);
      this.fetcher = fetcher;
      this.cacheName = cacheName;
   }

   private String getBestParam(String name) throws SyntaxError {
      String param;
      if (localParams != null) {
         param = localParams.get(name);
         if (param != null) return param;
         param = localParams.get(PARAM_PREFIX + name);
         if (param != null) return param;
      }
      param = params.get(PARAM_PREFIX + name);
      if (param != null) return param;
      param = params.get(name);
      if (param != null) return param;
      throw new SyntaxError("missing " + name + " parameter for TravelTime request");
   }

   @Override
   public TraveltimeSearchQuery parse() throws SyntaxError {
      float weight;
      try {
         weight = Float.parseFloat(getBestParam(WEIGHT));
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
          getBestParam(TraveltimeQueryParameters.FIELD),
          getBestParam(TraveltimeQueryParameters.ORIGIN),
          getBestParam(TraveltimeQueryParameters.LIMIT),
          getBestParam(TraveltimeQueryParameters.MODE),
          getBestParam(TraveltimeQueryParameters.COUNTRY)
      );
      return new TraveltimeSearchQuery(params, weight, fetcher, cacheName);
   }

}
