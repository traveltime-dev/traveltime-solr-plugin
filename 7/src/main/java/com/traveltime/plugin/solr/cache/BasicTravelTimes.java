package com.traveltime.plugin.solr.cache;

import com.traveltime.sdk.dto.common.Coordinates;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.StampedLock;
import lombok.val;

public class BasicTravelTimes extends TravelTimes {
  private final StampedLock rwLock = new StampedLock();
  private final Object2IntOpenHashMap<Coordinates> coordsToTimes = new Object2IntOpenHashMap<>();

  @Override
  public Set<Coordinates> nonCached(int ignored, ObjectCollection<Coordinates> coords) {
    long read = rwLock.readLock();
    try {
      val nonCachedSet = new ObjectOpenHashSet<Coordinates>();
      coords.forEach(
          coord -> {
            if (!coordsToTimes.containsKey(coord)) {
              nonCachedSet.add(coord);
            }
          });
      return nonCachedSet;
    } finally {
      rwLock.unlock(read);
    }
  }

  @Override
  public void putAll(int ignored, ArrayList<Coordinates> coords, List<Integer> times) {
    long write = rwLock.writeLock();
    try {
      for (int index = 0; index < times.size(); index++) {
        coordsToTimes.put(coords.get(index), times.get(index).intValue());
      }
    } finally {
      rwLock.unlock(write);
    }
  }

  @Override
  public Object2IntOpenHashMap<Coordinates> mapToTimes(
      int ignored, ObjectCollection<Coordinates> coords) {
    long read = rwLock.readLock();
    try {
      val pointToTime = new Object2IntOpenHashMap<Coordinates>(coords.size());
      coords.forEach(
          coord -> {
            int time = coordsToTimes.getOrDefault(coord, -1);
            if (time > 0) {
              pointToTime.put(coord, time);
            }
          });

      return pointToTime;
    } finally {
      rwLock.unlock(read);
    }
  }

  @Override
  public int get(Coordinates coord) {
    long read = rwLock.tryOptimisticRead();
    int time = coordsToTimes.getOrDefault(coord, -1);
    if (!rwLock.validate(read)) {
      read = rwLock.readLock();
      try {
        time = coordsToTimes.getOrDefault(coord, -1);
      } finally {
        rwLock.unlock(read);
      }
    }
    return time;
  }
}
