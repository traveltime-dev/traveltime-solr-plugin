package com.traveltime.plugin.solr.cache;

import com.traveltime.plugin.solr.query.TraveltimeQueryParameters;

public class ExactRequestCache extends RequestCache {
   private final Object[] lock = new Object[0];

   @Override
   public TravelTimes getOrFresh(TraveltimeQueryParameters key) {
      TravelTimes result = get(key);
      if (result == null) {
         synchronized (lock) {
            result = get(key);
            if (result == null) {
               result = put(key, new BasicTravelTimes());
            }
         }
      }
      return result;
   }
}