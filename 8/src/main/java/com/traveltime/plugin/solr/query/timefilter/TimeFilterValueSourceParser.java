package com.traveltime.plugin.solr.query.timefilter;

import com.traveltime.plugin.solr.cache.RequestCache;
import com.traveltime.plugin.solr.query.ParamSource;
import com.traveltime.plugin.solr.query.TraveltimeValueSource;
import lombok.val;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.FunctionQParser;
import org.apache.solr.search.SyntaxError;
import org.apache.solr.search.ValueSourceParser;

public class TimeFilterValueSourceParser extends ValueSourceParser {
   private String cacheName = RequestCache.NAME;

   @Override
   public void init(NamedList args) {
      super.init(args);
      Object cache = args.get("cache");
      if(cache != null) cacheName = cache.toString();
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

      ParamSource params = new ParamSource(fp.getParams());

      val arrivalTime = params.getOptionalParam(TimeFilterQueryParameters.ARRIVAL_TIME);
      val arrivalLocation = params.getOptionalParam(TimeFilterQueryParameters.ARRIVAL_LOCATION);

      val departureTime = params.getOptionalParam(TimeFilterQueryParameters.DEPARTURE_TIME);
      val departureLocation = params.getOptionalParam(TimeFilterQueryParameters.DEPARTURE_LOCATION);

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
                 params.getParam(TimeFilterQueryParameters.FIELD),
                 params.getParam(TimeFilterQueryParameters.ARRIVAL_LOCATION),
                 params.getParam(TimeFilterQueryParameters.ARRIVAL_TIME),
                 params.getParam(TimeFilterQueryParameters.TRAVEL_TIME),
                 params.getParam(TimeFilterQueryParameters.TRANSPORTATION),
                 params.getOptionalParam(TimeFilterQueryParameters.RANGE)
         );
      } else {
         queryParams = TimeFilterQueryParameters.fromStrings(
                 req.getSchema(),
                 TimeFilterQueryParameters.SearchType.DEPARTURE,
                 params.getParam(TimeFilterQueryParameters.FIELD),
                 params.getParam(TimeFilterQueryParameters.DEPARTURE_LOCATION),
                 params.getParam(TimeFilterQueryParameters.DEPARTURE_TIME),
                 params.getParam(TimeFilterQueryParameters.TRAVEL_TIME),
                 params.getParam(TimeFilterQueryParameters.TRANSPORTATION),
                 params.getOptionalParam(TimeFilterQueryParameters.RANGE)
         );
      }

      return new TraveltimeValueSource<>(queryParams, cache.getOrFresh(queryParams));
   }
}
