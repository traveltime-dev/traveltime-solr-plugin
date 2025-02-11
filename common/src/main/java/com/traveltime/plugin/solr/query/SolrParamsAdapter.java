package com.traveltime.plugin.solr.query;

public interface SolrParamsAdapter<A, E extends Exception> {
  String get(A a, String name);

  E exception(String message);
}
