package com.traveltime.plugin.solr;

public enum FetcherSingleton {
   INSTANCE;

   private ProtoFetcher underlying = null;
   private final Object[] lock = new Object[0];

   public void init(String id, String key) {
      if(underlying != null) return;
      synchronized (lock) {
         if(underlying != null) return;
         underlying = new ProtoFetcher(id, key);
      }
   }

   public ProtoFetcher getFetcher() {
      return underlying;
   }

}
