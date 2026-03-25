package com.traveltime.plugin.solr.cache;

import static org.junit.jupiter.api.Assertions.*;

import com.traveltime.sdk.dto.common.Coordinates;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import java.util.*;
import org.junit.jupiter.api.Test;

class LRUDataTest {

  private static Coordinates coord(int i) {
    return new Coordinates(51.0 + i * 0.01, -0.1);
  }

  private static LRUData createCache(int size, boolean onlyPositive) {
    Map<String, String> args = new HashMap<>();
    args.put("secondary_size", String.valueOf(size));
    args.put("only_positive", String.valueOf(onlyPositive));
    return new LRUData(args);
  }

  private static ObjectCollection<Coordinates> coordList(Coordinates... coords) {
    return new ObjectArrayList<>(coords);
  }

  private static ArrayList<Coordinates> destList(Coordinates... coords) {
    return new ArrayList<>(Arrays.asList(coords));
  }

  @Test
  void unreachableCoordsAreStoredWithMinusLimitOnFirstFetch() {
    LRUData data = createCache(100, false);
    Coordinates c = coord(0);
    int limit = 1000;

    data.putAll(limit, destList(c), List.of(-1));

    // Should be cached: -limit stored, so -1000 > -1000 is false
    Set<Coordinates> nc = data.nonCached(limit, coordList(c));
    assertTrue(nc.isEmpty(), "Coord stored as -limit should be considered cached");
  }

  @Test
  void unreachableCoordsWithSmallerLimitAreRefetched() {
    LRUData data = createCache(100, false);
    Coordinates c = coord(0);

    // Store as unreachable with limit=500 → stored as -500
    data.putAll(500, destList(c), List.of(-1));

    // Query with larger limit: -500 > -1000 → should re-fetch
    Set<Coordinates> nc = data.nonCached(1000, coordList(c));
    assertFalse(nc.isEmpty(), "Coord unreachable at limit=500 should be re-fetched at limit=1000");
  }

  @Test
  void unreachableCoordsWithSameLimitAreNotRefetched() {
    LRUData data = createCache(100, false);
    Coordinates c = coord(0);

    data.putAll(1000, destList(c), List.of(-1));

    Set<Coordinates> nc = data.nonCached(1000, coordList(c));
    assertTrue(
        nc.isEmpty(), "Coord unreachable at limit=1000 should not be re-fetched at same limit");
  }

  @Test
  void reachableCoordsAreCached() {
    LRUData data = createCache(100, false);
    Coordinates c = coord(0);

    data.putAll(1000, destList(c), List.of(500));

    Set<Coordinates> nc = data.nonCached(1000, coordList(c));
    assertTrue(nc.isEmpty());

    CoordToIntMap result = data.mapToData(1000, coordList(c));
    assertEquals(1, result.size());
    assertEquals(500, result.getOrDefault(c, -1));
  }

  @Test
  void mapToDataExcludesTimesOverLimit() {
    LRUData data = createCache(100, false);
    Coordinates c = coord(0);

    data.putAll(1000, destList(c), List.of(800));

    // Query with smaller limit: 800 > 500 → excluded
    CoordToIntMap result = data.mapToData(500, coordList(c));
    assertTrue(result.size() == 0, "Time 800 should be excluded when limit is 500");
  }

  @Test
  void onlyPositiveSkipsNegativeTimes() {
    LRUData data = createCache(100, true);
    Coordinates reachable = coord(0);
    Coordinates unreachable = coord(1);

    data.putAll(1000, destList(reachable, unreachable), List.of(500, -1));

    // Reachable should be cached
    Set<Coordinates> nc = data.nonCached(1000, coordList(reachable));
    assertTrue(nc.isEmpty(), "Reachable coord should be cached");

    // Unreachable should not be cached (was skipped)
    nc = data.nonCached(1000, coordList(unreachable));
    assertFalse(nc.isEmpty(), "Unreachable coord should not be cached when only_positive=true");
  }

  @Test
  void mapToDataIncludesZeroTravelTime() {
    LRUData data = createCache(100, false);
    Coordinates c = coord(0);

    data.putAll(1000, destList(c), List.of(0));

    CoordToIntMap result = data.mapToData(1000, coordList(c));
    assertEquals(1, result.size(), "Zero travel time should be included in results");
    assertEquals(0, result.getOrDefault(c, -1));
  }

  @Test
  void onlyPositiveStoresZeroTravelTime() {
    LRUData data = createCache(100, true);
    Coordinates c = coord(0);

    data.putAll(1000, destList(c), List.of(0));

    Set<Coordinates> nc = data.nonCached(1000, coordList(c));
    assertTrue(nc.isEmpty(), "Zero travel time should be cached when only_positive=true");
  }

  @Test
  void onlyPositivePreventsUnreachableFromEvictingReachable() {
    int cacheSize = 5;
    LRUData data = createCache(cacheSize, true);
    int limit = 1000;

    // Fill cache with 5 reachable coords
    ArrayList<Coordinates> reachable = new ArrayList<>();
    List<Integer> reachableTimes = new ArrayList<>();
    for (int i = 0; i < cacheSize; i++) {
      reachable.add(coord(i));
      reachableTimes.add(100 + i);
    }
    data.putAll(limit, reachable, reachableTimes);

    // Now add 10 unreachable coords — with only_positive these are skipped
    ArrayList<Coordinates> unreachable = new ArrayList<>();
    List<Integer> unreachableTimes = new ArrayList<>();
    for (int i = cacheSize; i < cacheSize + 10; i++) {
      unreachable.add(coord(i));
      unreachableTimes.add(-1);
    }
    data.putAll(limit, unreachable, unreachableTimes);

    // All 5 reachable should still be in cache
    ObjectCollection<Coordinates> all = new ObjectArrayList<>(reachable);
    CoordToIntMap result = data.mapToData(limit, all);
    assertEquals(
        cacheSize, result.size(), "All reachable coords should survive when only_positive=true");
  }

  @Test
  void withoutOnlyPositiveUnreachableCanEvictReachable() {
    int cacheSize = 5;
    LRUData data = createCache(cacheSize, false);
    int limit = 1000;

    // Fill cache with 5 reachable coords
    ArrayList<Coordinates> reachable = new ArrayList<>();
    List<Integer> reachableTimes = new ArrayList<>();
    for (int i = 0; i < cacheSize; i++) {
      reachable.add(coord(i));
      reachableTimes.add(100 + i);
    }
    data.putAll(limit, reachable, reachableTimes);

    // Add 10 unreachable coords — these WILL evict reachable entries
    ArrayList<Coordinates> unreachable = new ArrayList<>();
    List<Integer> unreachableTimes = new ArrayList<>();
    for (int i = cacheSize; i < cacheSize + 10; i++) {
      unreachable.add(coord(i));
      unreachableTimes.add(-1);
    }
    data.putAll(limit, unreachable, unreachableTimes);

    ObjectCollection<Coordinates> all = new ObjectArrayList<>(reachable);
    CoordToIntMap result = data.mapToData(limit, all);
    assertTrue(
        result.size() < cacheSize,
        "Some reachable coords should be evicted when only_positive=false and cache is full");
  }
}
