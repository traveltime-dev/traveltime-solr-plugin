package com.traveltime.plugin.solr.fetcher;

import com.traveltime.plugin.solr.Util;
import com.traveltime.plugin.solr.query.TravelTimeQueryParameters;
import com.traveltime.sdk.TravelTimeSDK;
import com.traveltime.sdk.auth.TravelTimeCredentials;
import com.traveltime.sdk.dto.common.Coordinates;
import com.traveltime.sdk.dto.requests.TimeFilterFastProtoRequest;
import com.traveltime.sdk.dto.responses.errors.IOError;
import com.traveltime.sdk.dto.responses.errors.ResponseError;
import com.traveltime.sdk.dto.responses.errors.TravelTimeError;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtoFetcher implements Fetcher<TravelTimeQueryParameters> {
  private final TravelTimeSDK api;

  private final Logger log = LoggerFactory.getLogger(ProtoFetcher.class);

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

  public ProtoFetcher(URI uri, String id, String key) {
    val auth = TravelTimeCredentials.builder().appId(id).apiKey(key).build();
    val builder = TravelTimeSDK.builder().credentials(auth);
    if (uri != null) {
      builder.baseProtoUri(uri);
    }
    api = builder.build();
  }

  @Override
  public Response getTimesDistances(
      TravelTimeQueryParameters params, ArrayList<Coordinates> destinations) {
    val fastProto =
        TimeFilterFastProtoRequest.builder()
            .requestType(params.getRequestType())
            .country(params.getCountry())
            .transportation(params.getMode())
            .originCoordinate(params.getOrigin())
            .destinationCoordinates(destinations)
            .travelTime(params.getLimit())
            .withDistance(params.isDistances())
            .build();

    log.info("Fetching {} destinations", destinations.size());
    val result = Util.time(log, () -> api.sendProtoBatched(fastProto));

    return result.fold(
        err -> {
          logError(err);
          throw new RuntimeException(err.getMessage());
        },
        resp -> Response.of(resp.getTravelTimes(), resp.getDistances()));
  }
}
