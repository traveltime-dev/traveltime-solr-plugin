package com.traveltime.plugin.solr.cache;

import java.util.function.BiConsumer;
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
  }

  private final Function<P, TimesAndDistances> get;
  private final BiConsumer<P, TimesAndDistances> put;
  private final Function<P, P> keyMapper;
  private final Supplier<? extends CachedData> dataSupplier;

  private final Object[] lock = new Object[0];

  public TimesAndDistances getOrFresh(P key) {
    key = keyMapper.apply(key);
    TimesAndDistances result = get.apply(key);
    if (result == null) {
      synchronized (lock) {
        result = get.apply(key);
        if (result == null) {
          result = new TimesAndDistances(dataSupplier.get(), dataSupplier.get());
          put.accept(key, result);
        }
      }
    }
    return result;
  }
}
