package com.traveltime.plugin.solr.cache;

import com.traveltime.plugin.solr.query.TravelTimeQueryParameters;
import java.util.Map;
import org.apache.solr.search.CacheRegenerator;

public class FuzzyRequestCache extends RequestCache<TravelTimeQueryParameters> {
  private final Object[] lock = new Object[0];
  private Map<String, String> args;

  @Override
  public Object init(Map<String, String> args, Object persistence, CacheRegenerator regenerator) {
    this.args = args;
    return super.init(args, persistence, regenerator);
  }

  @Override
  public TravelTimes getOrFresh(TravelTimeQueryParameters key) {
    key =
        new TravelTimeQueryParameters(
            null, key.getOrigin(), 0, key.getMode(), null, key.getRequestType());
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
