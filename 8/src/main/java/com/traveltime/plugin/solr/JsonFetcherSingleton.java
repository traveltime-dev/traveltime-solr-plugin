package com.traveltime.plugin.solr;

import java.net.URI;

public enum JsonFetcherSingleton {
   INSTANCE;

   private JsonFetcher underlying = null;
   private final Object[] lock = new Object[0];

   public void init(URI uri, String id, String key) {
      if(underlying != null) return;
      synchronized (lock) {
         if(underlying != null) return;
         underlying = new JsonFetcher(uri, id, key);
      }
   }

   public JsonFetcher getFetcher() {
      return underlying;
   }

}