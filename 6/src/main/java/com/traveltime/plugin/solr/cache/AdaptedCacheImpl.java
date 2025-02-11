package com.traveltime.plugin.solr.cache;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.solr.search.FastLRUCache;
import org.apache.solr.search.NoOpRegenerator;

@RequiredArgsConstructor
public class AdaptedCacheImpl<K, V> implements AdaptedCache<K, V> {
  private final FastLRUCache<K, V> cache = new FastLRUCache<>();

  @Override
  public void init(Map<String, String> args) {
    cache.init(args, null, new NoOpRegenerator());
  }

  @Override
  public V get(K key) {
    return cache.get(key);
  }

  @Override
  public void put(K key, V value) {
    cache.put(key, value);
  }
}
