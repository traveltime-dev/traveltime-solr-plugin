package com.traveltime.plugin.solr.cache;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UnadaptedRequestCache<P> {
  private final Function<P, TravelTimes> get;
  private final BiConsumer<P, TravelTimes> put;
  private final Function<P, P> keyMapper;
  private final Supplier<? extends TravelTimes> timesSupplier;

  private final Object[] lock = new Object[0];

  public TravelTimes getOrFresh(P key) {
    key = keyMapper.apply(key);
    TravelTimes result = get.apply(key);
    if (result == null) {
      synchronized (lock) {
        result = get.apply(key);
        if (result == null) {
          result = timesSupplier.get();
          put.accept(key, result);
        }
      }
    }
    return result;
  }
}
