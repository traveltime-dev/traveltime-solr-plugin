package com.traveltime.plugin.solr.cache;

import com.traveltime.plugin.solr.query.timefilter.TimeFilterQueryParameters;
import java.util.Map;
import lombok.Getter;
import org.apache.solr.search.CacheRegenerator;

@Getter
public class FuzzyTimeFilterRequestCache extends RequestCache<TimeFilterQueryParameters> {
  private Map<String, String> args;

  @Override
  public Object init(Map args, Object persistence, CacheRegenerator regenerator) {
    this.args = args;
    return super.init(args, persistence, regenerator);
  }

  private final UnadaptedRequestCache<TimeFilterQueryParameters> unadapted =
      new UnadaptedRequestCache<>(
          this::get,
          this::put,
          TimeFilterQueryParameters::fuzzy,
          () -> new LRUTimes(args, AdaptedCacheImpl::new));
}
