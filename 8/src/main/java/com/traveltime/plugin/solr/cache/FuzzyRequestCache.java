package com.traveltime.plugin.solr.cache;

import com.traveltime.plugin.solr.query.TravelTimeQueryParameters;
import java.util.Map;
import lombok.Getter;
import org.apache.solr.search.CacheRegenerator;

@Getter
public class FuzzyRequestCache extends RequestCache<TravelTimeQueryParameters> {
  private Map<String, String> args;

  @Override
  public Object init(Map args, Object persistence, CacheRegenerator regenerator) {
    this.args = args;
    return super.init(args, persistence, regenerator);
  }

  private final UnadaptedRequestCache<TravelTimeQueryParameters> unadapted =
      new UnadaptedRequestCache<>(
          this::get,
          this::put,
          TravelTimeQueryParameters::fuzzy,
          () -> new LRUTimes(args, AdaptedCacheImpl::new));
}
