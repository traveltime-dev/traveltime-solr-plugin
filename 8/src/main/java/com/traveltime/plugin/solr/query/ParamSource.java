package com.traveltime.plugin.solr.query;

import lombok.val;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.search.SyntaxError;

import java.util.Optional;

public class ParamSource {
   private final SolrParams[] params;

   public static final String PARAM_PREFIX = "traveltime_";

   public ParamSource(SolrParams... params) {
      this.params = params;
   }

   public String getParam(String name) throws SyntaxError {
      return getOptionalParam(name).orElseThrow(() -> new SyntaxError("missing " + name + " parameter for TravelTime request"));
   }

   public Optional<String> getOptionalParam(String name) {
      String param;
      for (val source : params) {
         param = source.get(PARAM_PREFIX + name);
         if (param != null) return Optional.of(param);
         param = source.get(name);
         if (param != null) return Optional.of(param);
      }
      return Optional.empty();
   }
}
