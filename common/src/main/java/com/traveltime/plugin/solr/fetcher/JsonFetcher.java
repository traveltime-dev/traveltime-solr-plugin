package com.traveltime.plugin.solr.fetcher;

import com.google.common.collect.Iterables;
import com.traveltime.plugin.solr.Util;
import com.traveltime.plugin.solr.query.timefilter.TimeFilterQueryParameters;
import com.traveltime.sdk.TravelTimeSDK;
import com.traveltime.sdk.auth.TravelTimeCredentials;
import com.traveltime.sdk.dto.common.Coordinates;
import com.traveltime.sdk.dto.common.Location;
import com.traveltime.sdk.dto.common.Property;
import com.traveltime.sdk.dto.requests.TimeFilterRequest;
import com.traveltime.sdk.dto.requests.timefilter.ArrivalSearch;
import com.traveltime.sdk.dto.requests.timefilter.DepartureSearch;
import com.traveltime.sdk.dto.responses.TimeFilterResponse;
import com.traveltime.sdk.dto.responses.errors.IOError;
import com.traveltime.sdk.dto.responses.errors.ResponseError;
import com.traveltime.sdk.dto.responses.errors.TravelTimeError;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import lombok.val;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonFetcher implements Fetcher<TimeFilterQueryParameters> {
  private final TravelTimeSDK api;

  private final int locationSizeLimit;

  private final Logger log = LoggerFactory.getLogger(JsonFetcher.class);

  private void logError(TravelTimeError left) {
    if (left instanceof IOError) {
      val ioerr = (IOError) left;
      log.warn(ioerr.getMessage());
      log.warn(
          Arrays.stream(ioerr.getCause().getStackTrace())
              .map(StackTraceElement::toString)
              .reduce("", (a, b) -> a + "\n\t" + b));
    } else if (left instanceof ResponseError) {
      val error = (ResponseError) left;
      log.warn(error.getDescription());
    }
  }

  public JsonFetcher(URI uri, String id, String key, int locationSizeLimit) {
    val auth = TravelTimeCredentials.builder().appId(id).apiKey(key).build();
    val client =
        new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.MINUTES)
            .callTimeout(5, TimeUnit.MINUTES)
            .readTimeout(5, TimeUnit.MINUTES)
            .build();
    val builder = TravelTimeSDK.builder().credentials(auth).client(client);
    if (uri != null) {
      builder.baseProtoUri(uri);
    }
    api = builder.build();
    this.locationSizeLimit = locationSizeLimit;
  }

  private Integer[] extractTimesDistances(
      TimeFilterResponse response, Integer[] travelTimes, Integer[] distances) {
    val result = response.getResults().get(0);

    result
        .getLocations()
        .forEach(
            location -> {
              travelTimes[Integer.parseInt(location.getId())] =
                  location.getProperties().get(0).getTravelTime();
              distances[Integer.parseInt(location.getId())] =
                  location.getProperties().get(0).getDistance();
            });

    result
        .getUnreachable()
        .forEach(unreachableId -> travelTimes[Integer.parseInt(unreachableId)] = -1);

    return travelTimes;
  }

  public Response getTimesDistances(
      TimeFilterQueryParameters parameters, ArrayList<Coordinates> points) {

    val locations =
        IntStream.range(0, points.size())
            .mapToObj(i -> new Location(String.valueOf(i), points.get(i)))
            .collect(Collectors.toList());

    val groupedLocations = Iterables.partition(locations, locationSizeLimit);

    List<Property> properties =
        parameters.isDistances()
            ? Arrays.asList(Property.TRAVEL_TIME, Property.DISTANCE)
            : Collections.singletonList(Property.TRAVEL_TIME);

    val requests =
        StreamSupport.stream(groupedLocations.spliterator(), true)
            .map(
                locationGroup -> {
                  val requestBuilder = TimeFilterRequest.builder();

                  requestBuilder.location(parameters.getLocation()).locations(locations);

                  switch (parameters.getSearchType()) {
                    case ARRIVAL:
                      val arrivalSearchBuilder =
                          ArrivalSearch.builder()
                              .id("search")
                              .arrivalLocationId(parameters.getLocation().getId())
                              .departureLocationIds(
                                  locations.stream()
                                      .map(Location::getId)
                                      .collect(Collectors.toList()))
                              .arrivalTime(parameters.getTime())
                              .travelTime(parameters.getTravelTime())
                              .properties(properties)
                              .transportation(parameters.getTransportation());
                      val arrivalSearch =
                          parameters
                              .getRange()
                              .map(arrivalSearchBuilder::range)
                              .orElse(arrivalSearchBuilder)
                              .build();
                      requestBuilder.arrivalSearch(arrivalSearch);
                      break;
                    case DEPARTURE:
                      val departureSearchBuilder =
                          DepartureSearch.builder()
                              .id("search")
                              .departureLocationId(parameters.getLocation().getId())
                              .arrivalLocationIds(
                                  locations.stream()
                                      .map(Location::getId)
                                      .collect(Collectors.toList()))
                              .departureTime(parameters.getTime())
                              .travelTime(parameters.getTravelTime())
                              .properties(properties)
                              .transportation(parameters.getTransportation());
                      val departureSearch =
                          parameters
                              .getRange()
                              .map(departureSearchBuilder::range)
                              .orElse(departureSearchBuilder)
                              .build();
                      requestBuilder.departureSearch(departureSearch);
                      break;
                  }

                  return requestBuilder.build();
                });

    log.info(String.format("Fetching %d locations", points.size()));

    Integer[] timeResults = new Integer[locations.size()];
    Integer[] distanceResults = new Integer[locations.size()];

    requests
        .map(request -> Util.time(log, () -> api.send(request)))
        .forEach(
            result ->
                result.fold(
                    err -> {
                      logError(err);
                      throw new RuntimeException(err.toString());
                    },
                    succ -> extractTimesDistances(succ, timeResults, distanceResults)));

    val times = Arrays.stream(timeResults).collect(Collectors.toList());
    val distances = Arrays.stream(distanceResults).collect(Collectors.toList());

    return Response.of(times, distances);
  }
}
