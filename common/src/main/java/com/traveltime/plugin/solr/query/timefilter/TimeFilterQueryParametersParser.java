package com.traveltime.plugin.solr.query.timefilter;

import com.traveltime.plugin.solr.query.ParamSource;
import com.traveltime.plugin.solr.query.SolrParamsAdapter;
import com.traveltime.sdk.dto.common.Coordinates;
import com.traveltime.sdk.dto.common.FullRange;
import com.traveltime.sdk.dto.common.Location;
import com.traveltime.sdk.dto.common.transportation.Transportation;
import com.traveltime.sdk.dto.responses.errors.TravelTimeError;
import com.traveltime.sdk.utils.JsonUtils;
import io.vavr.control.Either;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor
public class TimeFilterQueryParametersParser<A, E extends Exception> {
  private final Function<String, Optional<E>> fieldValidator;

  private <T> T getOrThrow(SolrParamsAdapter<A, E> adapter, Either<TravelTimeError, T> either)
      throws E {
    if (either.isLeft()) {
      throw adapter.exception(either.getLeft().getMessage());
    } else {
      return either.get();
    }
  }

  public TimeFilterQueryParameters parse(ParamSource<A, E> paramSource) throws E {
    val adapter = paramSource.adapter;

    String field = paramSource.getParam(TimeFilterQueryParameters.FIELD);

    val fieldValidationException = fieldValidator.apply(field);
    if (fieldValidationException.isPresent()) {
      throw fieldValidationException.get();
    }

    val arrivalTime = paramSource.getOptionalParam(TimeFilterQueryParameters.ARRIVAL_TIME);
    val arrivalLocation = paramSource.getOptionalParam(TimeFilterQueryParameters.ARRIVAL_LOCATION);

    val departureTime = paramSource.getOptionalParam(TimeFilterQueryParameters.DEPARTURE_TIME);
    val departureLocation =
        paramSource.getOptionalParam(TimeFilterQueryParameters.DEPARTURE_LOCATION);

    if (arrivalTime.isPresent() != arrivalLocation.isPresent()) {
      throw adapter.exception(
          String.format(
              "Only one of [%s, %s] was defined. Either both must be defined or none.",
              TimeFilterQueryParameters.ARRIVAL_TIME, TimeFilterQueryParameters.ARRIVAL_LOCATION));
    }

    if (departureTime.isPresent() != departureLocation.isPresent()) {
      throw adapter.exception(
          String.format(
              "Only one of [%s, %s] was defined. Either both must be defined or none.",
              TimeFilterQueryParameters.DEPARTURE_TIME,
              TimeFilterQueryParameters.DEPARTURE_LOCATION));
    }

    if (arrivalTime.isPresent() == departureTime.isPresent()) {
      throw adapter.exception(
          "You must provide exactly one of the parameter sets of arrival time/location or departure"
              + " time/location, but not both or neither.");
    }

    val transportation =
        JsonUtils.fromJson(
            paramSource.getParam(TimeFilterQueryParameters.TRANSPORTATION), Transportation.class);

    val range =
        paramSource
            .getOptionalParam(TimeFilterQueryParameters.RANGE)
            .map(r -> JsonUtils.fromJson(r, FullRange.class));
    Optional<FullRange> rangeOptional =
        range.isPresent() ? Optional.of(getOrThrow(adapter, range.get())) : Optional.empty();

    int travelTime;
    try {
      travelTime = Integer.parseInt(paramSource.getParam(TimeFilterQueryParameters.TRAVEL_TIME));
    } catch (NumberFormatException e) {
      throw adapter.exception("Couldn't parse traveltime limit as an integer");
    }
    if (travelTime <= 0) {
      throw adapter.exception("traveltime limit must be > 0");
    }

    TimeFilterQueryParameters queryParams;
    if (arrivalTime.isPresent()) {
      val locationCoords =
          JsonUtils.fromJson(
              paramSource.getParam(TimeFilterQueryParameters.ARRIVAL_LOCATION), Coordinates.class);
      val time = Instant.parse(paramSource.getParam(TimeFilterQueryParameters.ARRIVAL_TIME));

      val location = new Location("location", getOrThrow(adapter, locationCoords));

      queryParams =
          new TimeFilterQueryParameters(
              field,
              location,
              time,
              travelTime,
              getOrThrow(adapter, transportation),
              rangeOptional,
              TimeFilterQueryParameters.SearchType.ARRIVAL,
              false);
    } else {
      val locationCoords =
          JsonUtils.fromJson(
              paramSource.getParam(TimeFilterQueryParameters.DEPARTURE_LOCATION),
              Coordinates.class);
      val time = Instant.parse(paramSource.getParam(TimeFilterQueryParameters.DEPARTURE_TIME));

      val location = new Location("location", getOrThrow(adapter, locationCoords));

      queryParams =
          new TimeFilterQueryParameters(
              field,
              location,
              time,
              travelTime,
              getOrThrow(adapter, transportation),
              rangeOptional,
              TimeFilterQueryParameters.SearchType.DEPARTURE,
              false);
    }
    return queryParams;
  }
}
