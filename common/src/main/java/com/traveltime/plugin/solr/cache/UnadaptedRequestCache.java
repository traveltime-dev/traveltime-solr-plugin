package com.traveltime.plugin.solr.cache;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UnadaptedRequestCache<P> {
  @RequiredArgsConstructor
  @Getter
  public static class TimesAndDistances {
    private final CachedData times;
    private final CachedData distances;

    public long ramBytesUsed() {
      return times.ramBytesUsed() + distances.ramBytesUsed();
    }
  }

  private final Function<P, TimesAndDistances> get;
  private final BiConsumer<P, TimesAndDistances> put;
  private final Function<P, P> keyMapper;
  private final Supplier<? extends CachedData> dataSupplier;
  private final BiFunction<CachedData, CachedData, TimesAndDistances> timesAndDistancesFactory;

  private final Object[] lock = new Object[0];

  public TimesAndDistances getOrFresh(P key) {
    key = keyMapper.apply(key);
    TimesAndDistances result = get.apply(key);
    if (result == null) {
      synchronized (lock) {
        result = get.apply(key);
        if (result == null) {
          result = timesAndDistancesFactory.apply(dataSupplier.get(), dataSupplier.get());
          put.accept(key, result);
        }
      }
    }
    return result;
  }
}
