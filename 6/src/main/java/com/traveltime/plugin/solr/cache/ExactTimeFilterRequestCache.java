package com.traveltime.plugin.solr.cache;

import com.traveltime.plugin.solr.query.timefilter.TimeFilterQueryParameters;

public class ExactTimeFilterRequestCache extends RequestCache<TimeFilterQueryParameters> {
  private final ExactUnadaptedRequestCache<TimeFilterQueryParameters> unadapted =
      new ExactUnadaptedRequestCache<>(this::get, this::put);

  @Override
  public TravelTimes getOrFresh(TimeFilterQueryParameters key) {
    return unadapted.getOrFresh(key);
  }
}
