package com.traveltime.plugin.solr.cache;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class ExactUnadaptedRequestCache<P> extends UnadaptedCache<P> {
  private final Object[] lock = new Object[0];

  public ExactUnadaptedRequestCache(Function<P, TravelTimes> get, BiConsumer<P, TravelTimes> put) {
    super(get, put);
  }

  @Override
  public TravelTimes getOrFresh(P key) {
    TravelTimes result = get.apply(key);
    if (result == null) {
      synchronized (lock) {
        result = get.apply(key);
        if (result == null) {
          result = new BasicTravelTimes();
          put.accept(key, result);
        }
      }
    }
    return result;
  }
}
