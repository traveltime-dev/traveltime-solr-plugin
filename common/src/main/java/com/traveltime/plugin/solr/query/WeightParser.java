package com.traveltime.plugin.solr.query;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class WeightParser<E extends Exception> {
  private static final String WEIGHT = "weight";

  public float parseWeight(ParamSource<?, E> paramSource) throws E {
    float weight;
    try {
      weight = paramSource.getOptionalParam(WEIGHT).map(Float::parseFloat).orElse(0f);
    } catch (NumberFormatException e) {
      throw paramSource.adapter.exception("Couldn't parse traveltime weight as a float");
    }

    if (weight < 0 || weight > 1) {
      throw paramSource.adapter.exception("TravelTime weight must be between 0 and 1");
    }
    return weight;
  }
}
