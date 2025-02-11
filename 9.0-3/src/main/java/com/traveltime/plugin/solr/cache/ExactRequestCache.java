package com.traveltime.plugin.solr.cache;

import com.traveltime.plugin.solr.query.TravelTimeQueryParameters;
import java.util.function.Function;
import lombok.Getter;

@Getter
public class ExactRequestCache extends RequestCache<TravelTimeQueryParameters> {
  private final UnadaptedRequestCache<TravelTimeQueryParameters> unadapted =
      new UnadaptedRequestCache<>(this::get, this::put, Function.identity(), BasicTravelTimes::new);
}
