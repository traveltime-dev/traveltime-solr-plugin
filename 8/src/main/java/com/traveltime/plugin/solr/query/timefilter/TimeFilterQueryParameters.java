package com.traveltime.plugin.solr.query.timefilter;

import com.traveltime.plugin.solr.query.ParamSource;
import com.traveltime.plugin.solr.query.QueryParams;
import com.traveltime.sdk.dto.common.Coordinates;
import com.traveltime.sdk.dto.common.FullRange;
import com.traveltime.sdk.dto.common.Location;
import com.traveltime.sdk.dto.common.transportation.Transportation;
import com.traveltime.sdk.dto.responses.errors.TravelTimeError;
import com.traveltime.sdk.utils.JsonUtils;
import io.vavr.control.Either;
import lombok.Data;
import lombok.With;
import lombok.val;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.LatLonPointSpatialField;
import org.apache.solr.search.SyntaxError;

import java.time.Instant;
import java.util.Optional;

@Data
public class TimeFilterQueryParameters implements QueryParams {
   @With
   private final String field;
   private final Location location;
   private final Instant time;
   @With
   private final int travelTime;
   private final Transportation transportation;
   private final Optional<FullRange> range;
   private final SearchType searchType;

   public enum SearchType {
      ARRIVAL, DEPARTURE
   }

   public static final String FIELD = "field";

   public static final String DEPARTURE_LOCATION = "departure_location";
   public static final String DEPARTURE_TIME = "departure_time";

   public static final String ARRIVAL_LOCATION = "arrival_location";
   public static final String ARRIVAL_TIME = "arrival_time";

   public static final String TRAVEL_TIME = "travel_time";
   public static final String TRANSPORTATION = "transportation";
   public static final String RANGE = "range";

   private static <T> T getOrThrow(Either<TravelTimeError, T> either) throws SyntaxError {
      if (either.isLeft()) {
         throw new SyntaxError(either.getLeft().getMessage());
      } else {
         return either.get();
      }
   }

   public static TimeFilterQueryParameters parse(
         IndexSchema schema,
         ParamSource paramSource
   ) throws SyntaxError {

      String field = paramSource.getParam(TimeFilterQueryParameters.FIELD);
      if (!(schema.getField(field).getType() instanceof LatLonPointSpatialField)) {
         throw new SyntaxError("field[" + field + "] is not a LatLonPointSpatialField");
      }

      val arrivalTime = paramSource.getOptionalParam(TimeFilterQueryParameters.ARRIVAL_TIME);
      val arrivalLocation = paramSource.getOptionalParam(TimeFilterQueryParameters.ARRIVAL_LOCATION);

      val departureTime = paramSource.getOptionalParam(TimeFilterQueryParameters.DEPARTURE_TIME);
      val departureLocation = paramSource.getOptionalParam(TimeFilterQueryParameters.DEPARTURE_LOCATION);

      if (arrivalTime.isPresent() != arrivalLocation.isPresent()) {
         throw new SyntaxError(String.format("Only one of [%s, %s] was defined. Either both must be defined or none.",
                                             TimeFilterQueryParameters.ARRIVAL_TIME,
                                             TimeFilterQueryParameters.ARRIVAL_LOCATION
         ));
      }

      if (departureTime.isPresent() != departureLocation.isPresent()) {
         throw new SyntaxError(String.format("Only one of [%s, %s] was defined. Either both must be defined or none.",
                                             TimeFilterQueryParameters.DEPARTURE_TIME,
                                             TimeFilterQueryParameters.DEPARTURE_LOCATION
         ));
      }

      if (arrivalTime.isPresent() == departureTime.isPresent()) {
         throw new SyntaxError(
               "You must provide exactly one of the parameter sets of arrival time/location or departure time/location, but not both or neither.");
      }

      val transportation = JsonUtils.fromJson(paramSource.getParam(TimeFilterQueryParameters.TRANSPORTATION),
                                              Transportation.class
      );

      val range = paramSource.getOptionalParam(TimeFilterQueryParameters.RANGE)
                             .map(r -> JsonUtils.fromJson(r, FullRange.class));
      Optional<FullRange> rangeOptional = range.isPresent() ? Optional.of(getOrThrow(range.get())) : Optional.empty();

      int travelTime;
      try {
         travelTime = Integer.parseInt(paramSource.getParam(TimeFilterQueryParameters.TRAVEL_TIME));
      } catch (NumberFormatException e) {
         throw new SyntaxError("Couldn't parse traveltime limit as an integer");
      }
      if (travelTime <= 0) {
         throw new SyntaxError("traveltime limit must be > 0");
      }

      TimeFilterQueryParameters queryParams;
      if (arrivalTime.isPresent()) {
         val locationCoords = JsonUtils.fromJson(paramSource.getParam(TimeFilterQueryParameters.ARRIVAL_LOCATION),
                                                 Coordinates.class
         );
         val time = Instant.parse(paramSource.getParam(TimeFilterQueryParameters.ARRIVAL_TIME));

         val location = new Location("location", getOrThrow(locationCoords));

         queryParams = new TimeFilterQueryParameters(
               field,
               location,
               time,
               travelTime,
               getOrThrow(transportation),
               rangeOptional,
               SearchType.ARRIVAL
         );
      } else {
         val locationCoords = JsonUtils.fromJson(paramSource.getParam(TimeFilterQueryParameters.DEPARTURE_LOCATION),
                                                 Coordinates.class
         );
         val time = Instant.parse(paramSource.getParam(TimeFilterQueryParameters.DEPARTURE_TIME));

         val location = new Location("location", getOrThrow(locationCoords));

         queryParams = new TimeFilterQueryParameters(
               field,
               location,
               time,
               travelTime,
               getOrThrow(transportation),
               rangeOptional,
               SearchType.DEPARTURE
         );
      }
      return queryParams;
   }

    @Override
    public Coordinates getOrigin() {
        return location.getCoords();
    }

    @Override
    public String getTransportMode() {
        return transportation == null ? "-" : transportation.toString();
    }
}
