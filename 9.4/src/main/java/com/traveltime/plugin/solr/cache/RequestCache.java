package com.traveltime.plugin.solr.cache;

import java.util.Map;
import org.apache.solr.search.CacheRegenerator;
import org.apache.solr.search.CaffeineCache;
import org.apache.solr.search.NoOpRegenerator;

public abstract class RequestCache<P> extends CaffeineCache<P, TravelTimes> {
  public static final String NAME = "traveltime";

  public abstract TravelTimes getOrFresh(P key);

  @Override
  public void init(Map<String, String> args, CacheRegenerator ignored) {
    super.init(args, new NoOpRegenerator());
  }
}
