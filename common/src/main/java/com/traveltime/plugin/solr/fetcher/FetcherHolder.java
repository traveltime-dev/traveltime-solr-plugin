package com.traveltime.plugin.solr.fetcher;

import java.util.function.Supplier;

public class FetcherHolder<F> {
  private volatile F fetcher;

  public F getOrCreate(Supplier<F> factory) {
    if (fetcher == null) {
      synchronized (this) {
        if (fetcher == null) {
          fetcher = factory.get();
        }
      }
    }
    return fetcher;
  }
}
