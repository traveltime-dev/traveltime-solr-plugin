package com.traveltime.plugin.solr.query.timefilter;

import com.traveltime.plugin.solr.query.QueryParams;
import com.traveltime.sdk.dto.common.Coordinates;
import com.traveltime.sdk.dto.common.FullRange;
import com.traveltime.sdk.dto.common.Location;
import com.traveltime.sdk.dto.common.transportation.Transportation;
import java.time.Instant;
import java.util.Optional;
import lombok.Data;
import lombok.With;

@Data
public class TimeFilterQueryParameters implements QueryParams {
  @With private final String field;
  private final Location location;
  private final Instant time;
  @With private final int travelTime;
  private final Transportation transportation;
  private final Optional<FullRange> range;
  private final SearchType searchType;

  public enum SearchType {
    ARRIVAL,
    DEPARTURE
  }

  public static final String FIELD = "field";

  public static final String DEPARTURE_LOCATION = "departure_location";
  public static final String DEPARTURE_TIME = "departure_time";

  public static final String ARRIVAL_LOCATION = "arrival_location";
  public static final String ARRIVAL_TIME = "arrival_time";

  public static final String TRAVEL_TIME = "travel_time";
  public static final String TRANSPORTATION = "transportation";
  public static final String RANGE = "range";

  @Override
  public Coordinates getOrigin() {
    return location.getCoords();
  }

  @Override
  public String getTransportMode() {
    return transportation == null ? "-" : transportation.toString();
  }
}
