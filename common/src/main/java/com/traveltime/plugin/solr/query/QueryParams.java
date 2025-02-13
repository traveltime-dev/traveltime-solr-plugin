package com.traveltime.plugin.solr.query;

import com.traveltime.sdk.dto.common.Coordinates;

public interface QueryParams<A extends QueryParams<A>> {
  String getField();

  int getTravelTime();

  Coordinates getOrigin();

  String getTransportMode();

  boolean isDistances();

  A fuzzy();
}
