package com.traveltime.plugin.solr.cache;

import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class UnadaptedCache<P> {
  protected final Function<P, TravelTimes> get;
  protected final BiConsumer<P, TravelTimes> put;

  public abstract TravelTimes getOrFresh(P key);
}
