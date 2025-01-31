package com.traveltime.plugin.solr.fetcher;

import com.traveltime.sdk.dto.common.Coordinates;
import java.util.ArrayList;
import java.util.List;

public interface Fetcher<Params> {
  List<Integer> getTimes(Params parameters, ArrayList<Coordinates> points);
}
