package com.traveltime.plugin.solr.cache;

import com.traveltime.sdk.dto.common.Coordinates;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.val;
import org.apache.solr.search.FastLRUCache;
import org.apache.solr.search.NoOpRegenerator;

public class LRUTimes extends TravelTimes {
  private final FastLRUCache<Coordinates, Integer> coordsToTimes = new FastLRUCache<>();

  public LRUTimes(Map<String, String> args) {
    args.putIfAbsent("name", "fuzzy_cache");
    String size = args.get("secondary_size");
    if (size == null) {
      size = "10000";
    }
    args.put("size", size);

    coordsToTimes.init(args, null, new NoOpRegenerator());
  }

  @Override
  public Set<Coordinates> nonCached(int limit, ObjectCollection<Coordinates> coords) {
    val nonCachedSet = new ObjectOpenHashSet<Coordinates>();
    coords.forEach(
        coord -> {
          Integer time = coordsToTimes.get(coord);
          if (time == null || (time < 0 && time > -limit)) {
            nonCachedSet.add(coord);
          }
        });
    return nonCachedSet;
  }

  @Override
  public void putAll(int limit, ArrayList<Coordinates> coords, List<Integer> times) {
    for (int index = 0; index < times.size(); index++) {
      int time = times.get(index);
      if (time < 0) {
        Integer stored = coordsToTimes.get(coords.get(index));
        if (stored != null && stored < 0) {
          time = Math.min(-limit, stored);
        }
      }
      coordsToTimes.put(coords.get(index), time);
    }
  }

  @Override
  public Object2IntOpenHashMap<Coordinates> mapToTimes(
      int limit, ObjectCollection<Coordinates> coords) {
    val pointToTime = new Object2IntOpenHashMap<Coordinates>(coords.size());
    coords.forEach(
        coord -> {
          Integer time = coordsToTimes.get(coord);
          if (time != null && time > 0 && time <= limit) {
            pointToTime.put(coord, time.intValue());
          }
        });

    return pointToTime;
  }

  @Override
  public int get(Coordinates coord) {
    Integer time = coordsToTimes.get(coord);
    if (time == null || time < 0) return -1;
    return time;
  }
}
