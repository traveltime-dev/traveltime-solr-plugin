package com.traveltime.plugin.solr;

import com.traveltime.plugin.solr.util.Util;
import com.traveltime.sdk.TravelTimeSDK;
import com.traveltime.sdk.auth.TravelTimeCredentials;
import com.traveltime.sdk.dto.common.Coordinates;
import com.traveltime.sdk.dto.requests.TimeFilterFastProtoRequest;
import com.traveltime.sdk.dto.requests.proto.Country;
import com.traveltime.sdk.dto.requests.proto.OneToMany;
import com.traveltime.sdk.dto.requests.proto.Transportation;
import com.traveltime.sdk.dto.responses.TimeFilterFastProtoResponse;
import com.traveltime.sdk.dto.responses.errors.IOError;
import com.traveltime.sdk.dto.responses.errors.ResponseError;
import com.traveltime.sdk.dto.responses.errors.TravelTimeError;
import lombok.val;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

public class ProtoFetcher {
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
      if(uri != null) {
         builder.baseProtoUri(uri);
      }
      api = builder.build();
   }

   public List<Integer> getTimes(Coordinates origin, List<Coordinates> destinations, int limit, Transportation mode, Country country) {
      val fastProto =
              TimeFilterFastProtoRequest
                      .builder()
                      .oneToMany(
                              OneToMany
                                      .builder()
                                      .country(country)
                                      .transportation(mode)
                                      .originCoordinate(origin)
                                      .destinationCoordinates(destinations)
                                      .travelTime(limit)
                                      .build()
                      )
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
