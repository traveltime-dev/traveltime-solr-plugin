package com.traveltime.plugin.solr.cache;

import java.util.Map;
import org.apache.solr.search.CacheRegenerator;
import org.apache.solr.search.FastLRUCache;
import org.apache.solr.util.SolrPluginUtils;

public abstract class RequestCache<P> extends FastLRUCache<P, TravelTimes> {
  public static final String NAME = "traveltime";

  public abstract TravelTimes getOrFresh(P key);

  @Override
  public void init(Map<String, String> args, CacheRegenerator ignored) {
    super.init(args, new SolrPluginUtils.IdentityRegenerator());
  }
}
