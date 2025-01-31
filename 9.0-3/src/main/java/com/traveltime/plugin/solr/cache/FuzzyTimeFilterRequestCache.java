package com.traveltime.plugin.solr.cache;

import com.traveltime.plugin.solr.query.timefilter.TimeFilterQueryParameters;
import java.util.Map;
import org.apache.solr.search.CacheRegenerator;

public class FuzzyTimeFilterRequestCache extends RequestCache<TimeFilterQueryParameters> {
  private final Object[] lock = new Object[0];
  private Map<String, String> args;

  @Override
  public Object init(Map<String, String> args, Object persistence, CacheRegenerator regenerator) {
    this.args = args;
    return super.init(args, persistence, regenerator);
  }

  @Override
  public TravelTimes getOrFresh(TimeFilterQueryParameters key) {
    key = key.withField(null).withTravelTime(0);
    TravelTimes result = get(key);
    if (result == null) {
      synchronized (lock) {
        result = get(key);
        if (result == null) {
          result = new LRUTimes(args);
          put(key, result);
        }
      }
    }
    return result;
  }
}
