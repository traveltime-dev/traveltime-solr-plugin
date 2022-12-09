package com.traveltime.plugin.solr.cache;

import com.traveltime.plugin.solr.query.TraveltimeQueryParameters;
import org.apache.solr.search.CacheRegenerator;

import java.util.Map;

public class FuzzyRequestCache extends RequestCache {
   private final Object[] lock = new Object[0];
   private Map<String, String> args;

   @Override
   public Object init(Map args, Object persistence, CacheRegenerator regenerator) {
      this.args = args;
      return super.init(args, persistence, regenerator);
   }

      @Override
   public TravelTimes getOrFresh(TraveltimeQueryParameters key) {
      key = new TraveltimeQueryParameters(null, key.getOrigin(), 0, key.getMode(), null);
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
