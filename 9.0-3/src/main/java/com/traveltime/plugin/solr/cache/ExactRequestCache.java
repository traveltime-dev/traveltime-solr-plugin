package com.traveltime.plugin.solr.cache;

import com.traveltime.plugin.solr.query.TravelTimeQueryParameters;

public class ExactRequestCache extends RequestCache<TravelTimeQueryParameters> {
   private final Object[] lock = new Object[0];

   @Override
   public TravelTimes getOrFresh(TravelTimeQueryParameters key) {
      TravelTimes result = get(key);
      if (result == null) {
         synchronized (lock) {
            result = get(key);
            if (result == null) {
               result = new BasicTravelTimes();
               put(key, result);
            }
         }
      }
      return result;
   }
}