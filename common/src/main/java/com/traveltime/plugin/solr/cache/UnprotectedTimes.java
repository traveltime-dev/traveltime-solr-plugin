package com.traveltime.plugin.solr.cache;

import com.traveltime.sdk.dto.common.Coordinates;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// Only for use within TravelTimeDelegatingCollector if no cache is set up
public class UnprotectedTimes extends TravelTimes {
  private final Object2IntOpenHashMap<Coordinates> coordsToTimes = new Object2IntOpenHashMap<>();

  public Set<Coordinates> nonCached(int ignored, ObjectCollection<Coordinates> coords) {
    return new ObjectOpenHashSet<>(coords);
  }

  public void putAll(int ignored, ArrayList<Coordinates> coords, List<Integer> times) {
    for (int index = 0; index < times.size(); index++) {
      if (times.get(index) > 0) {
        coordsToTimes.put(coords.get(index), times.get(index).intValue());
      }
    }
  }

  public Object2IntOpenHashMap<Coordinates> mapToTimes(
      int ignored, ObjectCollection<Coordinates> coords) {
    return coordsToTimes;
  }

  @Override
  public int get(Coordinates coord) {
    return -1;
  }
}
