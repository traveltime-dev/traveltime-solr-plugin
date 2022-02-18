package com.traveltime.plugin.solr.cache;

import com.traveltime.plugin.solr.query.TraveltimeQueryParameters;

public class FuzzyRequestCache extends RequestCache {
   private final Object[] lock = new Object[0];

   @Override
   public TravelTimes getOrFresh(TraveltimeQueryParameters key) {
      key = new TraveltimeQueryParameters(null, key.getOrigin(), 0, key.getMode(), null);
      TravelTimes result = get(key);
      if (result == null) {
         synchronized (lock) {
            result = get(key);
            if (result == null) {
               result = new LRUTimes();
               put(key, result);
            }
         }
      }
      return result;
   }
}
