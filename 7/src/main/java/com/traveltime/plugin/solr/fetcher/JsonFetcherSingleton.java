package com.traveltime.plugin.solr.fetcher;

import java.net.URI;

public enum JsonFetcherSingleton {
   INSTANCE;

   private JsonFetcher underlying = null;
   private final Object[] lock = new Object[0];

   public void init(URI uri, String id, String key, int locationSizeLimit) {
      if (underlying != null) return;
      synchronized (lock) {
         if (underlying != null) return;
         underlying = new JsonFetcher(uri, id, key, locationSizeLimit);
      }
   }

   public JsonFetcher getFetcher() {
      return underlying;
   }

}
