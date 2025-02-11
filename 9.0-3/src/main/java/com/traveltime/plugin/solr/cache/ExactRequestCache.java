package com.traveltime.plugin.solr.cache;

import com.traveltime.plugin.solr.query.TravelTimeQueryParameters;

public class ExactRequestCache extends RequestCache<TravelTimeQueryParameters> {
  private final ExactUnadaptedRequestCache<TravelTimeQueryParameters> unadapted =
      new ExactUnadaptedRequestCache<>(this::get, this::put);

  @Override
  public TravelTimes getOrFresh(TravelTimeQueryParameters key) {
    return unadapted.getOrFresh(key);
  }
}
