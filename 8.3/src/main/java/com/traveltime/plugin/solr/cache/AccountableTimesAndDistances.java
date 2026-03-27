package com.traveltime.plugin.solr.cache;

import org.apache.lucene.util.Accountable;

public class AccountableTimesAndDistances extends UnadaptedRequestCache.TimesAndDistances
    implements Accountable {
  public AccountableTimesAndDistances(CachedData times, CachedData distances) {
    super(times, distances);
  }
}
