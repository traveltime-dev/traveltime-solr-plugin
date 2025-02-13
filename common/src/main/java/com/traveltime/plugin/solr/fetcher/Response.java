package com.traveltime.plugin.solr.fetcher;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class Response {
  private final List<Integer> times;
  private final List<Integer> distances;

  public static Response empty() {
    return new Response(new ArrayList<>(), new ArrayList<>());
  }

  static Response of(List<Integer> times, List<Integer> distances) {
    return new Response(times, distances);
  }
}
