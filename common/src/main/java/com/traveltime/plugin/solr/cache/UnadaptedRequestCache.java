package com.traveltime.plugin.solr.cache;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UnadaptedRequestCache<P> {
  private final Function<P, CachedData> get;
  private final BiConsumer<P, CachedData> put;
  private final Function<P, P> keyMapper;
  private final Supplier<? extends CachedData> dataSupplier;

  private final Object[] lock = new Object[0];

  public CachedData getOrFresh(P key) {
    key = keyMapper.apply(key);
    CachedData result = get.apply(key);
    if (result == null) {
      synchronized (lock) {
        result = get.apply(key);
        if (result == null) {
          result = dataSupplier.get();
          put.accept(key, result);
        }
      }
    }
    return result;
  }
}
