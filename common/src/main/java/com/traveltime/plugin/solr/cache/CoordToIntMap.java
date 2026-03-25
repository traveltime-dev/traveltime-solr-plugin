package com.traveltime.plugin.solr.cache;

import com.traveltime.sdk.dto.common.Coordinates;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

/**
 * A Coordinates → int map backed by a primitive Long2IntOpenHashMap. Coordinates are encoded to
 * longs via {@link CoordEncoding}, avoiding object overhead per entry.
 */
public class CoordToIntMap {
  private final Long2IntOpenHashMap map;

  CoordToIntMap() {
    map = new Long2IntOpenHashMap();
  }

  CoordToIntMap(int expectedSize) {
    map = new Long2IntOpenHashMap(expectedSize);
  }

  public int getOrDefault(Coordinates coord, int defaultValue) {
    return map.getOrDefault(CoordEncoding.encode(coord), defaultValue);
  }

  public boolean containsKey(Coordinates coord) {
    return map.containsKey(CoordEncoding.encode(coord));
  }

  public int size() {
    return map.size();
  }

  void put(Coordinates coord, int value) {
    map.put(CoordEncoding.encode(coord), value);
  }
}
