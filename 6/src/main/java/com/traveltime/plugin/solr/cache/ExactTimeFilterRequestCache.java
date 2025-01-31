package com.traveltime.plugin.solr.cache;

import com.traveltime.plugin.solr.query.timefilter.TimeFilterQueryParameters;

public class ExactTimeFilterRequestCache extends RequestCache<TimeFilterQueryParameters> {
  private final Object[] lock = new Object[0];

  @Override
  public TravelTimes getOrFresh(TimeFilterQueryParameters key) {
    TravelTimes result = get(key);
    if (result == null) {
      synchronized (lock) {
        result = get(key);
        if (result == null) {
          result = new BasicTravelTimes();
          put(key, result);
        }
      }
    }
    return result;
  }
}
