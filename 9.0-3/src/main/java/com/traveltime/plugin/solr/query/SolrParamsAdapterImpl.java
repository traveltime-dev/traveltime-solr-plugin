package com.traveltime.plugin.solr.query;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.search.SyntaxError;

public enum SolrParamsAdapterImpl implements SolrParamsAdapter<SolrParams, SyntaxError> {
  INSTANCE;

  @Override
  public String get(SolrParams a, String name) {
    return a.get(name);
  }

  @Override
  public SyntaxError exception(String message) {
    return new SyntaxError(message);
  }
}
