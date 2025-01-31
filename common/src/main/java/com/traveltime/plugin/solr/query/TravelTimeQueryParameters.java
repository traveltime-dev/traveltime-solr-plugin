package com.traveltime.plugin.solr.query;

import com.traveltime.sdk.dto.common.Coordinates;
import com.traveltime.sdk.dto.requests.proto.Country;
import com.traveltime.sdk.dto.requests.proto.RequestType;
import com.traveltime.sdk.dto.requests.proto.Transportation;
import lombok.Data;
import lombok.With;

@Data
public class TravelTimeQueryParameters implements QueryParams {
  private final String field;
  private final Coordinates origin;
  private final int limit;
  @With private final Transportation mode;
  @With private final Country country;
  @With private final RequestType requestType;

  @Override
  public int getTravelTime() {
    return limit;
  }

  public static final String FIELD = "field";
  public static final String ORIGIN = "origin";
  public static final String MODE = "mode";
  public static final String LIMIT = "limit";
  public static final String COUNTRY = "country";
  public static final String REQUEST_TYPE = "requestType";

  @Override
  public String getTransportMode() {
    return mode == null ? "-" : mode.getValue();
  }
}
