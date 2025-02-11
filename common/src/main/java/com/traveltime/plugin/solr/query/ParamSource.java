package com.traveltime.plugin.solr.query;

import java.util.Optional;
import lombok.val;

public class ParamSource<A, E extends Exception> {
  public final SolrParamsAdapter<A, E> adapter;

  private final String paramPrefix;

  private final A[] params;

  public static final String PARAM_PREFIX = "traveltime_";

  public ParamSource(SolrParamsAdapter<A, E> adapter, String paramPrefix, A... params) {
    this.adapter = adapter;
    this.paramPrefix = paramPrefix;
    this.params = params;
  }

  public String getParam(String name) throws E {
    return getOptionalParam(name)
        .orElseThrow(
            () -> adapter.exception("missing " + name + " parameter for TravelTime request"));
  }

  public Optional<String> getOptionalParam(String name) {
    String param;
    for (val source : params) {
      param = adapter.get(source, paramPrefix + name);
      if (param != null) return Optional.of(param);
      param = adapter.get(source, name);
      if (param != null) return Optional.of(param);
    }
    return Optional.empty();
  }
}
