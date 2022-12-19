package com.traveltime.plugin.solr.query.timefilter;

import com.traveltime.plugin.solr.cache.RequestCache;
import com.traveltime.plugin.solr.util.Util;
import lombok.val;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.FunctionQParser;
import org.apache.solr.search.SyntaxError;
import org.apache.solr.search.ValueSourceParser;

import java.util.Optional;

public class TimeFilterValueSourceParser extends ValueSourceParser {
   private String cacheName = RequestCache.NAME;

   @Override
   public void init(NamedList args) {
      super.init(args);
      Object cache = args.get("cache");
      if(cache != null) cacheName = cache.toString();
   }

   private String getParam(SolrParams params, String name) throws SyntaxError {
      String param = Util.getParam(params, name);
      Util.assertPresent(param, name);
      return param;
   }

   private Optional<String> getOptionalParam(SolrParams params, String name) {
      String param = Util.getParam(params, name);
      return Optional.ofNullable(param);
   }


   @Override
   public ValueSource parse(FunctionQParser fp) throws SyntaxError {
      SolrQueryRequest req = fp.getReq();
      RequestCache<TimeFilterQueryParameters> cache = (RequestCache<TimeFilterQueryParameters>) req.getSearcher().getCache(cacheName);
      if (cache == null) {
         throw new SolrException(
             SolrException.ErrorCode.BAD_REQUEST,
             "No request cache configured."
         );
      }

      SolrParams params = fp.getParams();

      val arrivalTime = getOptionalParam(params, TimeFilterQueryParameters.ARRIVAL_TIME);
      val arrivalLocation = getOptionalParam(params, TimeFilterQueryParameters.ARRIVAL_LOCATION);

      val departureTime = getOptionalParam(params, TimeFilterQueryParameters.DEPARTURE_TIME);
      val departureLocation = getOptionalParam(params, TimeFilterQueryParameters.DEPARTURE_LOCATION);

      if(arrivalTime.isPresent() != arrivalLocation.isPresent()) {
         throw new SyntaxError(String.format("Only one of [%s, %s] was defined. Either both must be defined or none.", TimeFilterQueryParameters.ARRIVAL_TIME, TimeFilterQueryParameters.ARRIVAL_LOCATION));
      }

      if(departureTime.isPresent() != departureLocation.isPresent()) {
         throw new SyntaxError(String.format("Only one of [%s, %s] was defined. Either both must be defined or none.", TimeFilterQueryParameters.DEPARTURE_TIME, TimeFilterQueryParameters.DEPARTURE_LOCATION));
      }

      if(arrivalTime.isPresent() == departureTime.isPresent()) {
         throw new SyntaxError("You must provide exactly one of the parameter sets of arrival time/location or departure time/location, but not both or neither.");
      }

      TimeFilterQueryParameters queryParams;
      if (arrivalTime.isPresent()) {
         queryParams = TimeFilterQueryParameters.fromStrings(
                 req.getSchema(),
                 TimeFilterQueryParameters.SearchType.ARRIVAL,
                 getParam(params, TimeFilterQueryParameters.FIELD),
                 getParam(params, TimeFilterQueryParameters.ARRIVAL_LOCATION),
                 getParam(params, TimeFilterQueryParameters.ARRIVAL_TIME),
                 getParam(params, TimeFilterQueryParameters.TRAVEL_TIME),
                 getParam(params, TimeFilterQueryParameters.TRANSPORTATION),
                 getParam(params, TimeFilterQueryParameters.RANGE)
         );
      } else {
         queryParams = TimeFilterQueryParameters.fromStrings(
                 req.getSchema(),
                 TimeFilterQueryParameters.SearchType.DEPARTURE,
                 getParam(params, TimeFilterQueryParameters.FIELD),
                 getParam(params, TimeFilterQueryParameters.DEPARTURE_LOCATION),
                 getParam(params, TimeFilterQueryParameters.DEPARTURE_TIME),
                 getParam(params, TimeFilterQueryParameters.TRAVEL_TIME),
                 getParam(params, TimeFilterQueryParameters.TRANSPORTATION),
                 getParam(params, TimeFilterQueryParameters.RANGE)
         );
      }

      return new TimeFilterValueSource(queryParams, cache.getOrFresh(queryParams));
   }
}
