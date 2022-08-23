package com.traveltime.plugin.solr.cache;

import com.traveltime.plugin.solr.query.TraveltimeQueryParameters;
import org.apache.solr.search.CacheRegenerator;
import org.apache.solr.search.FastLRUCache;
import org.apache.solr.search.NoOpRegenerator;

import java.util.Map;

public abstract class RequestCache extends FastLRUCache<TraveltimeQueryParameters, TravelTimes> {
   public static String NAME = "traveltime";

   public abstract TravelTimes getOrFresh(TraveltimeQueryParameters key);

   @Override
   public void init(Map<String, String> args, CacheRegenerator ignored) {
      super.init(args, new NoOpRegenerator());
   }

}