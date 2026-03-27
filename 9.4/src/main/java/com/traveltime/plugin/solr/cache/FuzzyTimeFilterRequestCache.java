package com.traveltime.plugin.solr.cache;

import com.traveltime.plugin.solr.query.timefilter.TimeFilterQueryParameters;
import java.util.Map;
import lombok.Getter;
import org.apache.solr.search.CacheRegenerator;

@Getter
public class FuzzyTimeFilterRequestCache extends RequestCache<TimeFilterQueryParameters> {
  private Map<String, String> args;

  @Override
  public Object init(Map<String, String> args, Object persistence, CacheRegenerator regenerator) {
    this.args = args;
    return super.init(args, persistence, regenerator);
  }

  private final UnadaptedRequestCache<TimeFilterQueryParameters> unadapted =
      new UnadaptedRequestCache<>(
          this::get, this::put, TimeFilterQueryParameters::fuzzy, () -> new LRUData(args));

  @Override
  public String getName() {
    return getClass().getName();
  }

  @Override
  public String getDescription() {
    return String.format(
        "TravelTime FuzzyTimeFilterRequestCache(secondary_size=%s) - %s",
        LRUData.getSecondarySize(args), super.getDescription());
  }
}
