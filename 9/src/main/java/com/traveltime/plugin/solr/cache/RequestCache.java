package com.traveltime.plugin.solr.cache;

import org.apache.solr.search.CacheRegenerator;
import org.apache.solr.search.CaffeineCache;
import org.apache.solr.search.NoOpRegenerator;

import java.util.Map;

public abstract class RequestCache<P> extends CaffeineCache<P, TravelTimes> {
   public static String NAME = "traveltime";

   public abstract TravelTimes getOrFresh(P key);

   @Override
   public void init(Map<String, String> args, CacheRegenerator ignored) {
      super.init(args, new NoOpRegenerator());
   }

}