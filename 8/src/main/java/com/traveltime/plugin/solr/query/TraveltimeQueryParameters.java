package com.traveltime.plugin.solr.query;

import com.traveltime.plugin.solr.util.Util;
import com.traveltime.sdk.dto.common.Coordinates;
import com.traveltime.sdk.dto.requests.proto.Country;
import com.traveltime.sdk.dto.requests.proto.Transportation;
import lombok.Data;
import lombok.With;
import lombok.val;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.LatLonPointSpatialField;
import org.apache.solr.search.SyntaxError;

import java.util.Optional;
import java.util.function.Function;

@Data
public class TraveltimeQueryParameters implements QueryParams {
   private final String field;
   private final Coordinates origin;
   private final int limit;
   @With
   private final Transportation mode;
   @With
   private final Country country;

   @Override
   public int getTravelTime() {
      return limit;
   }

   public static final String FIELD = "field";
   public static final String ORIGIN = "origin";
   public static final String MODE = "mode";
   public static final String LIMIT = "limit";
   public static final String COUNTRY = "country";

   private static <T> T findByNameOrError(String what, String name, Function<String, Optional<T>> finder) {
      val result = finder.apply(name);
      if (!result.isPresent()) {
         throw new IllegalArgumentException(String.format("Couldn't find a %s with the name %s", what, name));
      } else {
         return result.get();
      }
   }

   public static TraveltimeQueryParameters parse(
         IndexSchema schema,
         ParamSource params
   ) throws SyntaxError {
      String field = params.getParam(TraveltimeQueryParameters.FIELD);

      val fieldType = schema.getField(field);
      if (!(fieldType.getType() instanceof LatLonPointSpatialField)) {
         throw new SyntaxError("field[" + field + "] is not a LatLonPointSpatialField");
      }

      val origin = Util.toGeoPoint(params.getParam(TraveltimeQueryParameters.ORIGIN));

      val mode = findByNameOrError("transportation mode",
                                   params.getParam(TraveltimeQueryParameters.MODE),
                                   Util::findModeByName
      );
      val country = findByNameOrError("country",
                                      params.getParam(TraveltimeQueryParameters.COUNTRY),
                                      Util::findCountryByName
      );

      int limit;
      try {
         limit = Integer.parseInt(params.getParam(TraveltimeQueryParameters.LIMIT));
      } catch (NumberFormatException e) {
         throw new SyntaxError("Couldn't parse traveltime limit as an integer");
      }
      if (limit <= 0) {
         throw new SyntaxError("traveltime limit must be > 0");
      }

      return new TraveltimeQueryParameters(field, origin, limit, mode, country);
   }

   @Override
   public String getTransportMode() {
      return mode == null ? "-" : mode.getValue();
   }
}
