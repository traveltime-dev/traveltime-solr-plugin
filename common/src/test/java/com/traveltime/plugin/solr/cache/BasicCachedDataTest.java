package com.traveltime.plugin.solr.cache;

import static org.junit.jupiter.api.Assertions.*;

import com.traveltime.sdk.dto.common.Coordinates;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BasicCachedDataTest {

  private static Coordinates coord(int i) {
    return new Coordinates(51.0 + i * 0.01, -0.1);
  }

  private static ObjectCollection<Coordinates> coordList(Coordinates... coords) {
    return new ObjectArrayList<>(coords);
  }

  private static ArrayList<Coordinates> destList(Coordinates... coords) {
    return new ArrayList<>(Arrays.asList(coords));
  }

  @Test
  void mapToDataIncludesZeroTravelTime() {
    BasicCachedData data = new BasicCachedData();
    Coordinates c = coord(0);

    data.putAll(0, destList(c), List.of(0));

    CoordToIntMap result = data.mapToData(0, coordList(c));
    assertEquals(1, result.size(), "Zero travel time should be included in results");
    assertEquals(0, result.getOrDefault(c, -1));
  }

  @Test
  void mapToDataExcludesNegativeTravelTime() {
    BasicCachedData data = new BasicCachedData();
    Coordinates c = coord(0);

    data.putAll(0, destList(c), List.of(-1));

    CoordToIntMap result = data.mapToData(0, coordList(c));
    assertTrue(result.size() == 0, "Negative travel time should be excluded");
  }

  @Test
  void reachableCoordsAreCached() {
    BasicCachedData data = new BasicCachedData();
    Coordinates c = coord(0);

    data.putAll(0, destList(c), List.of(500));

    Set<Coordinates> nc = data.nonCached(0, coordList(c));
    assertTrue(nc.isEmpty(), "Stored coord should be considered cached");
  }

  @Test
  void unreachableCoordsAreCached() {
    BasicCachedData data = new BasicCachedData();
    Coordinates c = coord(0);

    data.putAll(0, destList(c), List.of(-1));

    Set<Coordinates> nc = data.nonCached(0, coordList(c));
    assertTrue(nc.isEmpty(), "Unreachable coord should still be considered cached");
  }

  @Test
  void getReturnsMinusOneForUnknownCoord() {
    BasicCachedData data = new BasicCachedData();
    assertEquals(-1, data.get(coord(0)));
  }

  @Test
  void getReturnsStoredTime() {
    BasicCachedData data = new BasicCachedData();
    Coordinates c = coord(0);

    data.putAll(0, destList(c), List.of(42));

    assertEquals(42, data.get(c));
  }
}
