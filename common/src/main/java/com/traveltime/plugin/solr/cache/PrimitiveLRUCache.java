package com.traveltime.plugin.solr.cache;

import com.traveltime.sdk.dto.common.Coordinates;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;

/**
 * A bounded LRU cache mapping Coordinates to int, backed by a primitive {@link
 * Long2IntLinkedOpenHashMap}. Coordinates are encoded to longs via {@link CoordEncoding}, and
 * values are stored as primitive ints (~21 bytes/entry).
 *
 * <p>LRU eviction: on {@link #get}, accessed entries are moved to the tail. On {@link #put}, if the
 * map is full, the head (least recently used) entry is evicted.
 */
class PrimitiveLRUCache {
  static final int MISSING = Integer.MIN_VALUE;

  private final Long2IntLinkedOpenHashMap map;
  private final int maxSize;

  PrimitiveLRUCache(int maxSize) {
    this.maxSize = maxSize;
    this.map = new Long2IntLinkedOpenHashMap(maxSize);
    this.map.defaultReturnValue(MISSING);
  }

  int get(Coordinates key) {
    long encoded = CoordEncoding.encode(key);
    return map.getAndMoveToLast(encoded);
  }

  void put(Coordinates key, int value) {
    long encoded = CoordEncoding.encode(key);
    if (!map.containsKey(encoded) && map.size() >= maxSize) {
      map.removeFirstInt();
    }
    map.putAndMoveToLast(encoded, value);
  }

  long ramBytesUsed() {
    int n = HashCommon.arraySize(maxSize, Hash.DEFAULT_LOAD_FACTOR);
    int arrayLength = n + 1;
    // Long2IntLinkedOpenHashMap: long[] key + int[] value + long[] link, each of length n+1
    return (long) arrayLength * (Long.BYTES + Integer.BYTES + Long.BYTES);
  }
}
