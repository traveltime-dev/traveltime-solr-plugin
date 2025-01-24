package com.traveltime.plugin.solr.fetcher;

import com.traveltime.plugin.solr.query.TravelTimeQueryParameters;
import com.traveltime.plugin.solr.util.Util;
import com.traveltime.sdk.TravelTimeSDK;
import com.traveltime.sdk.auth.TravelTimeCredentials;
import com.traveltime.sdk.dto.common.Coordinates;
import com.traveltime.sdk.dto.requests.TimeFilterFastProtoRequest;
import com.traveltime.sdk.dto.responses.TimeFilterFastProtoResponse;
import com.traveltime.sdk.dto.responses.errors.IOError;
import com.traveltime.sdk.dto.responses.errors.ResponseError;
import com.traveltime.sdk.dto.responses.errors.TravelTimeError;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
                     .reduce("", (a, b) -> a + "\n\t" + b)
         );
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
   public List<Integer> getTimes(TravelTimeQueryParameters params, ArrayList<Coordinates> destinations) {
      val fastProto = TimeFilterFastProtoRequest
            .builder()
            .requestType(params.getRequestType())
            .country(params.getCountry())
            .transportation(params.getMode())
            .originCoordinate(params.getOrigin())
            .destinationCoordinates(destinations)
            .travelTime(params.getLimit())
            .build();


      log.info(String.format("Fetching %d destinations", destinations.size()));
      val result = Util.time(log, () -> api.sendProtoBatched(fastProto));

      return result.fold(
            err -> {
               logError(err);
               throw new RuntimeException(err.getMessage());
            },
            TimeFilterFastProtoResponse::getTravelTimes
      );
   }

}
