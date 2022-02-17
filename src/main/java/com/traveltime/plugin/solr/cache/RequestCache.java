package com.traveltime.plugin.solr.cache;

import com.traveltime.plugin.solr.query.TraveltimeQueryParameters;
import org.apache.solr.search.CacheRegenerator;
import org.apache.solr.search.FastLRUCache;
import org.apache.solr.util.SolrPluginUtils;

import java.util.Map;

public abstract class RequestCache extends FastLRUCache<TraveltimeQueryParameters, TravelTimes> {
   public static String NAME = "traveltime";
   private final Object[] lock = new Object[0];

   public abstract TravelTimes fresh();

   public TravelTimes getOrFresh(TraveltimeQueryParameters key) {
      TravelTimes result = get(key);
      if(result == null) {
         synchronized (lock) {
            result = get(key);
            if(result == null) {
               result = put(key, fresh());
            }
         }
      }
      return result;
   }

   @Override
   public void init(Map<String, String> args, CacheRegenerator ignored) {
      super.init(args, new SolrPluginUtils.IdentityRegenerator());
   }

}
