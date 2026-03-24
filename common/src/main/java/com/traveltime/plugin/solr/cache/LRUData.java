package com.traveltime.plugin.solr.cache;

import static com.traveltime.plugin.solr.cache.PrimitiveLRUCache.MISSING;

import com.traveltime.sdk.dto.common.Coordinates;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.val;

public class LRUData extends CachedData {
  public static final String SECONDARY_SIZE_PARAM = "secondary_size";
  public static final String DEFAULT_SECONDARY_SIZE = "10000";
  public static final String ONLY_POSITIVE_PARAM = "only_positive";

  public static String getSecondarySize(Map<String, String> args) {
    return args.getOrDefault(SECONDARY_SIZE_PARAM, DEFAULT_SECONDARY_SIZE);
  }

  private final PrimitiveLRUCache coordsToTimes;
  private final boolean onlyPositive;

  public LRUData(Map<String, String> args) {
    int size = Integer.parseInt(args.getOrDefault(SECONDARY_SIZE_PARAM, DEFAULT_SECONDARY_SIZE));
    onlyPositive = Boolean.parseBoolean(args.getOrDefault(ONLY_POSITIVE_PARAM, "false"));
    coordsToTimes = new PrimitiveLRUCache(size);
  }

  @Override
  public Set<Coordinates> nonCached(int limit, ObjectCollection<Coordinates> coords) {
    val nonCachedSet = new ObjectOpenHashSet<Coordinates>();
    coords.forEach(
        coord -> {
          int time = coordsToTimes.get(coord);
          if (time == MISSING || (time < 0 && time > -limit)) {
            nonCachedSet.add(coord);
          }
        });
    return nonCachedSet;
  }

  @Override
  public void putAll(int limit, ArrayList<Coordinates> coords, List<Integer> times) {
    for (int index = 0; index < times.size(); index++) {
      int time = times.get(index);
      if (onlyPositive && time < 0) continue;
      if (time < 0) {
        int stored = coordsToTimes.get(coords.get(index));
        if (stored == MISSING) {
          time = -limit;
        } else if (stored < 0) {
          time = Math.min(-limit, stored);
        }
      }
      coordsToTimes.put(coords.get(index), time);
    }
  }

  @Override
  public Object2IntOpenHashMap<Coordinates> mapToData(
      int limit, ObjectCollection<Coordinates> coords) {
    val pointToTime = new Object2IntOpenHashMap<Coordinates>(coords.size());
    coords.forEach(
        coord -> {
          int time = coordsToTimes.get(coord);
          if (time >= 0 && time <= limit) {
            pointToTime.put(coord, time);
          }
        });

    return pointToTime;
  }

  @Override
  public int get(Coordinates coord) {
    int time = coordsToTimes.get(coord);
    if (time < 0) return -1;
    return time;
  }
}
