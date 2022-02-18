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
public class TraveltimeQueryParameters {
   private final String field;
   private final Coordinates origin;
   private final int limit;
   @With private final Transportation mode;
   @With private final Country country;

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

   public static TraveltimeQueryParameters fromStrings(
       IndexSchema schema,
       String field,
       String originStr,
       String limitStr,
       String modeStr,
       String countryStr
       ) throws SyntaxError {

      val fieldType =  schema.getField(field);
      if (!(fieldType.getType() instanceof LatLonPointSpatialField)) {
         throw new SyntaxError("field[" + field + "] is not a LatLonPointSpatialField");
      }

      val origin = Util.toGeoPoint(originStr);

      val mode = findByNameOrError("transportation mode", modeStr, Util::findModeByName);
      val country = findByNameOrError("country", countryStr, Util::findCountryByName);

      int limit;
      try {
         limit = Integer.parseInt(limitStr);
      } catch (NumberFormatException e) {
         throw new SyntaxError("Couldn't parse traveltime limit as an integer");
      }
      if (limit <= 0) {
         throw new SyntaxError("traveltime limit must be > 0");
      }

      return null;
   }

}
