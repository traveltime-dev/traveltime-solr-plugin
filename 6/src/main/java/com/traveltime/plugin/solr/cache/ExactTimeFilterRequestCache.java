package com.traveltime.plugin.solr.cache;

import com.traveltime.plugin.solr.query.timefilter.TimeFilterQueryParameters;
import java.util.function.Function;
import lombok.Getter;

@Getter
public class ExactTimeFilterRequestCache extends RequestCache<TimeFilterQueryParameters> {
  private final UnadaptedRequestCache<TimeFilterQueryParameters> unadapted =
      new UnadaptedRequestCache<>(this::get, this::put, Function.identity(), BasicTravelTimes::new);
}
