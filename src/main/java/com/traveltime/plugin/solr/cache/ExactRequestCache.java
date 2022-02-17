package com.traveltime.plugin.solr.cache;

public class ExactRequestCache extends RequestCache {
   @Override
   public TravelTimes fresh() {
      return new BasicTravelTimes();
   }
}
