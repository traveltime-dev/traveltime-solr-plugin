package com.traveltime.plugin.solr.query.timefilter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.traveltime.sdk.dto.common.Coordinates;
import com.traveltime.sdk.dto.common.FullRange;
import com.traveltime.sdk.dto.common.Location;
import com.traveltime.sdk.dto.common.Property;
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
import java.util.List;
import java.util.Optional;

@Data
public class TimeFilterQueryParameters {
    @With private final String field;
    private final Location location;
    private final Instant time;
    @With private final int travelTime;
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

    private static final TypeReference<List<Property>> PROPERTY_LIST_TYPE = new TypeReference<List<Property>>() {
    };

    private static <T> T getOrThrow(Either<TravelTimeError, T> either) throws SyntaxError {
        if(either.isLeft()) {
            throw new SyntaxError(either.getLeft().getMessage());
        } else {
            return either.get();
        }
    }

    public static TimeFilterQueryParameters fromStrings(
            IndexSchema schema,
            SearchType searchType,
            String field,
            String locationStr,
            String timeStr,
            String travelTimeStr,
            String transportationStr,
            Optional<String> rangeStr
    ) throws SyntaxError {

        val fieldType = schema.getField(field);
        if (!(fieldType.getType() instanceof LatLonPointSpatialField)) {
            throw new SyntaxError("field[" + field + "] is not a LatLonPointSpatialField");
        }

        val locationCoords = JsonUtils.fromJson(locationStr, Coordinates.class);
        val time = Instant.parse(timeStr);
        val transportation = JsonUtils.fromJson(transportationStr, Transportation.class);
        val range = rangeStr.map(r ->  JsonUtils.fromJson(r, FullRange.class));

        val location = new Location("location", getOrThrow(locationCoords));

        int travelTime;
        try {
            travelTime = Integer.parseInt(travelTimeStr);
        } catch (NumberFormatException e) {
            throw new SyntaxError("Couldn't parse traveltime limit as an integer");
        }
        if (travelTime <= 0) {
            throw new SyntaxError("traveltime limit must be > 0");
        }

        Optional<FullRange> rangeOptional = range.isPresent() ? Optional.of(getOrThrow(range.get())) : Optional.empty();


        return new TimeFilterQueryParameters(
                field,
                location,
                time,
                travelTime,
                getOrThrow(transportation),
                rangeOptional,
                searchType
        );
    }

}
