package com.traveltime.plugin.solr.query;

import static com.traveltime.plugin.solr.Util.findByNameOrError;

import com.traveltime.plugin.solr.Util;
import com.traveltime.sdk.dto.common.Coordinates;
import com.traveltime.sdk.dto.requests.proto.RequestType;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor
public class TravelTimeQueryParametersParser<A, E extends Exception> {
  private final Function<String, Optional<E>> fieldValidator;
  private final Function<String, Coordinates> coordinatesParser;

  public TravelTimeQueryParameters parse(ParamSource<A, E> params) throws E {
    SolrParamsAdapter<A, E> adapter = params.adapter;
    String field = params.getParam(TravelTimeQueryParameters.FIELD);

    val fieldValidationException = fieldValidator.apply(field);
    if (fieldValidationException.isPresent()) {
      throw fieldValidationException.get();
    }

    val origin = coordinatesParser.apply(params.getParam(TravelTimeQueryParameters.ORIGIN));

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
      throw adapter.exception("Couldn't parse traveltime limit as an integer");
    }
    if (limit <= 0) {
      throw adapter.exception("traveltime limit must be > 0");
    }

    boolean distances;
    try {
      distances = params.getOptionalParam(TravelTimeQueryParameters.DISTANCES).map(Boolean::parseBoolean).orElse(false);
    } catch (NumberFormatException e) {
      throw adapter.exception("Couldn't parse distances as a boolean");
    }

    return new TravelTimeQueryParameters(
        field, origin, limit, mode, country, requestType, distances);
  }
}
