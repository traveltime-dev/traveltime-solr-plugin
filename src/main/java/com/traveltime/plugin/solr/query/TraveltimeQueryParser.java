package com.traveltime.plugin.solr.query;

import com.traveltime.plugin.solr.ProtoFetcher;
import com.traveltime.plugin.solr.util.Util;
import com.traveltime.sdk.dto.common.Coordinates;
import com.traveltime.sdk.dto.requests.proto.Country;
import com.traveltime.sdk.dto.requests.proto.Transportation;
import lombok.val;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.LatLonPointSpatialField;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.QParser;
import org.apache.solr.search.SyntaxError;

import java.util.Optional;
import java.util.function.Function;

public class TraveltimeQueryParser extends QParser {
   private static final String PARAM_PREFIX = "traveltime_";
   private static final String FIELD = "field";
   private static final String ORIGIN = "origin";
   private static final String MODE = "mode";
   private static final String LIMIT = "limit";
   private static final String COUNTRY = "country";
   private static final String WEIGHT = "weight";

   private final ProtoFetcher fetcher;

   public TraveltimeQueryParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req, ProtoFetcher fetcher) {
      super(qstr, localParams, params, req);
      this.fetcher = fetcher;
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

   private <T> T findByNameOrError(String what, String name, Function<String, Optional<T>> finder) {
      val result = finder.apply(name);
      if (!result.isPresent()) {
         throw new IllegalArgumentException(String.format("Couldn't find a %s with the name %s", what, name));
      } else {
         return result.get();
      }
   }

   @Override
   public TraveltimeSearchQuery parse() throws SyntaxError {
      val field = getBestParam(FIELD);

      val fieldType = req.getSchema().getField(field);
      if (!(fieldType.getType() instanceof LatLonPointSpatialField)) {
         throw new SyntaxError("field[" + field + "] is not a LatLonPointSpatialField");
      }

      val originStr = getBestParam(ORIGIN);
      val origin = Util.toGeoPoint(originStr);

      val mode = findByNameOrError("transportation mode", getBestParam(MODE), Util::findModeByName);
      val country = findByNameOrError("country", getBestParam(COUNTRY), Util::findCountryByName);

      int limit;
      try {
         limit = Integer.parseInt(getBestParam(LIMIT));
      } catch (NumberFormatException e) {
         throw new SyntaxError("Couldn't parse traveltime limit as an integer");
      }
      if (limit <= 0) {
         throw new SyntaxError("traveltime limit must be > 0");
      }


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

      val params = new TraveltimeQueryParameters(field, origin, limit, mode, country);
      return new TraveltimeSearchQuery(params, weight, fetcher);
   }

}
