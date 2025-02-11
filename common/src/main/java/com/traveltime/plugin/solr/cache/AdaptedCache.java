package com.traveltime.plugin.solr.cache;

import java.util.Map;

public interface AdaptedCache<K, V> {
  void init(Map<String, String> args);

  V get(K key);

  void put(K key, V value);
}
