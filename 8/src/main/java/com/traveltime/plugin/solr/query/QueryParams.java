package com.traveltime.plugin.solr.query;

import com.traveltime.sdk.dto.common.Coordinates;

public interface QueryParams {
   String getField();

   int getTravelTime();

    Coordinates getOrigin();

    String getTransportMode();
}
