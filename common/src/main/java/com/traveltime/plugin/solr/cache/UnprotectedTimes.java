package com.traveltime.plugin.solr.cache;

import com.traveltime.sdk.dto.common.Coordinates;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// Only for use within TravelTimeDelegatingCollector if no cache is set up
public class UnprotectedTimes extends CachedData {
  private final CoordToIntMap coordsToTimes = new CoordToIntMap();

  public Set<Coordinates> nonCached(int ignored, ObjectCollection<Coordinates> coords) {
    return new ObjectOpenHashSet<>(coords);
  }

  public void putAll(int ignored, ArrayList<Coordinates> coords, List<Integer> times) {
    for (int index = 0; index < times.size(); index++) {
      if (times.get(index) >= 0) {
        coordsToTimes.put(coords.get(index), times.get(index).intValue());
      }
    }
  }

  public CoordToIntMap mapToData(int ignored, ObjectCollection<Coordinates> coords) {
    return coordsToTimes;
  }

  @Override
  public long ramBytesUsed() {
    return coordsToTimes.ramBytesUsed();
  }

  @Override
  public int get(Coordinates coord) {
    return -1;
  }
}
