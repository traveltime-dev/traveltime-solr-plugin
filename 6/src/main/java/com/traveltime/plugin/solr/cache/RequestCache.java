package com.traveltime.plugin.solr.cache;

import java.util.Map;
import org.apache.solr.search.CacheRegenerator;
import org.apache.solr.search.FastLRUCache;
import org.apache.solr.util.SolrPluginUtils;

public abstract class RequestCache<P> extends FastLRUCache<P, CachedData> {
  public static final String NAME = "traveltime";

  protected abstract UnadaptedRequestCache<P> getUnadapted();

  public CachedData getOrFresh(P key) {
    return getUnadapted().getOrFresh(key);
  }

  @Override
  public void init(Map<String, String> args, CacheRegenerator ignored) {
    super.init(args, new SolrPluginUtils.IdentityRegenerator());
  }
}
