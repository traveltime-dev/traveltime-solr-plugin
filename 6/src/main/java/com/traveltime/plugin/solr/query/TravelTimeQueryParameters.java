package com.traveltime.plugin.solr.query;

import static com.traveltime.plugin.solr.util.Util.findByNameOrError;

import com.traveltime.plugin.solr.util.Util;
import com.traveltime.sdk.dto.common.Coordinates;
import com.traveltime.sdk.dto.requests.proto.Country;
import com.traveltime.sdk.dto.requests.proto.RequestType;
import com.traveltime.sdk.dto.requests.proto.Transportation;
import lombok.Data;
import lombok.With;
import lombok.val;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.LatLonPointSpatialField;
import org.apache.solr.search.SyntaxError;

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

  public static TravelTimeQueryParameters parse(IndexSchema schema, ParamSource params)
      throws SyntaxError {
    String field = params.getParam(TravelTimeQueryParameters.FIELD);

    val fieldType = schema.getField(field);
    if (!(fieldType.getType() instanceof LatLonPointSpatialField)) {
      throw new SyntaxError("field[" + field + "] is not a LatLonPointSpatialField");
    }

    val origin = Util.toGeoPoint(params.getParam(TravelTimeQueryParameters.ORIGIN));

    val mode =
        findByNameOrError(
            "transportation mode",
            params.getParam(TravelTimeQueryParameters.MODE),
            Util::findModeByName);
    val country =
        findByNameOrError(
            "country", params.getParam(TravelTimeQueryParameters.COUNTRY), Util::findCountryByName);
    val requestType =
        findByNameOrError(
            "request type",
            params
                .getOptionalParam(TravelTimeQueryParameters.REQUEST_TYPE)
                .orElse(RequestType.ONE_TO_MANY.name()),
            Util::findRequestTypeByName);

    int limit;
    try {
      limit = Integer.parseInt(params.getParam(TravelTimeQueryParameters.LIMIT));
    } catch (NumberFormatException e) {
      throw new SyntaxError("Couldn't parse traveltime limit as an integer");
    }
    if (limit <= 0) {
      throw new SyntaxError("traveltime limit must be > 0");
    }

    return new TravelTimeQueryParameters(field, origin, limit, mode, country, requestType);
  }

  @Override
  public String getTransportMode() {
    return mode == null ? "-" : mode.getValue();
  }
}
