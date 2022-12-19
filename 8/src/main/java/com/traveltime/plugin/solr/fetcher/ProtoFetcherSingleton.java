package com.traveltime.plugin.solr.fetcher;

import java.net.URI;

public enum ProtoFetcherSingleton {
   INSTANCE;

   private ProtoFetcher underlying = null;
   private final Object[] lock = new Object[0];

   public void init(URI uri, String id, String key) {
      if(underlying != null) return;
      synchronized (lock) {
         if(underlying != null) return;
         underlying = new ProtoFetcher(uri, id, key);
      }
   }

   public ProtoFetcher getFetcher() {
      return underlying;
   }

}
