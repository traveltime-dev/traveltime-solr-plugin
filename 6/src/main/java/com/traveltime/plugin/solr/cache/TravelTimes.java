package com.traveltime.plugin.solr.cache;

import com.traveltime.sdk.dto.common.Coordinates;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class TravelTimes {
  public abstract Set<Coordinates> nonCached(int limit, ObjectCollection<Coordinates> coords);

  public abstract void putAll(int limit, ArrayList<Coordinates> coords, List<Integer> times);

  public abstract Object2IntOpenHashMap<Coordinates> mapToTimes(
      int limit, ObjectCollection<Coordinates> coords);

  public abstract int get(Coordinates coord);
}
