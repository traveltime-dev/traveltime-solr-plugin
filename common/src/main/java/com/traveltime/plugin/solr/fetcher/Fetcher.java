package com.traveltime.plugin.solr.fetcher;

import com.traveltime.plugin.solr.cache.UnadaptedRequestCache;
import com.traveltime.plugin.solr.query.QueryParams;
import com.traveltime.sdk.dto.common.Coordinates;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import java.util.ArrayList;
import lombok.val;

public interface Fetcher<Params extends QueryParams<Params>> {
  Response getTimesDistances(Params parameters, ArrayList<Coordinates> points);

  default UnadaptedRequestCache.TimesAndDistances getWithCached(
      UnadaptedRequestCache.TimesAndDistances cachedResults,
      Params params,
      ObjectCollection<Coordinates> coords) {
    val nonCachedSet = cachedResults.getTimes().nonCached(params.getTravelTime(), coords);
    if (params.isDistances()) {
      nonCachedSet.addAll(cachedResults.getDistances().nonCached(params.getTravelTime(), coords));
    }

    ArrayList<Coordinates> destinations = new ArrayList<>(nonCachedSet);

    Response reponse;
    if (destinations.isEmpty()) {
      reponse = Response.empty();
    } else {
      reponse = this.getTimesDistances(params, destinations);
    }

    cachedResults.getTimes().putAll(params.getTravelTime(), destinations, reponse.getTimes());
    if (params.isDistances()) {
      cachedResults
          .getDistances()
          .putAll(params.getTravelTime(), destinations, reponse.getDistances());
    }

    return cachedResults;
  }
}
