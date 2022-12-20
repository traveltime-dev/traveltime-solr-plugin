package com.traveltime.plugin.solr.query.timefilter;

import com.traveltime.plugin.solr.fetcher.Fetcher;
import com.traveltime.plugin.solr.fetcher.JsonFetcher;
import com.traveltime.plugin.solr.query.ParamSource;
import com.traveltime.plugin.solr.query.TraveltimeSearchQuery;
import lombok.val;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.SyntaxError;

public class TimeFilterQueryParser extends QParser {
   private static final String WEIGHT = "weight";

   private final Fetcher<TimeFilterQueryParameters> fetcher;
   private final String cacheName;



   public TimeFilterQueryParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req, JsonFetcher fetcher, String cacheName) {
      super(qstr, localParams, params, req);
      this.fetcher = fetcher;
      this.cacheName = cacheName;
   }

   @Override
   public TraveltimeSearchQuery<TimeFilterQueryParameters> parse() throws SyntaxError {
      ParamSource paramSource = new ParamSource(localParams, params);
      float weight;
      try {
         weight = Float.parseFloat(paramSource.getParam(WEIGHT));
      } catch (SyntaxError ignored) {
         weight = 0f;
      } catch (NumberFormatException e) {
         throw new SyntaxError("Couldn't parse traveltime weight as a float");
      }
      if(weight < 0 || weight > 1) {
         throw new SyntaxError("Traveltime weight must be between 0 and 1");
      }

      val arrivalTime = paramSource.getOptionalParam(TimeFilterQueryParameters.ARRIVAL_TIME);
      val arrivalLocation = paramSource.getOptionalParam(TimeFilterQueryParameters.ARRIVAL_LOCATION);

      val departureTime = paramSource.getOptionalParam(TimeFilterQueryParameters.DEPARTURE_TIME);
      val departureLocation = paramSource.getOptionalParam(TimeFilterQueryParameters.DEPARTURE_LOCATION);

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
                 paramSource.getParam(TimeFilterQueryParameters.FIELD),
                 paramSource.getParam(TimeFilterQueryParameters.ARRIVAL_LOCATION),
                 paramSource.getParam(TimeFilterQueryParameters.ARRIVAL_TIME),
                 paramSource.getParam(TimeFilterQueryParameters.TRAVEL_TIME),
                 paramSource.getParam(TimeFilterQueryParameters.TRANSPORTATION),
                 paramSource.getOptionalParam(TimeFilterQueryParameters.RANGE)
         );
      } else {
         queryParams = TimeFilterQueryParameters.fromStrings(
                 req.getSchema(),
                 TimeFilterQueryParameters.SearchType.DEPARTURE,
                 paramSource.getParam(TimeFilterQueryParameters.FIELD),
                 paramSource.getParam(TimeFilterQueryParameters.DEPARTURE_LOCATION),
                 paramSource.getParam(TimeFilterQueryParameters.DEPARTURE_TIME),
                 paramSource.getParam(TimeFilterQueryParameters.TRAVEL_TIME),
                 paramSource.getParam(TimeFilterQueryParameters.TRANSPORTATION),
                 paramSource.getOptionalParam(TimeFilterQueryParameters.RANGE)
         );
      }

      return new TraveltimeSearchQuery<>(queryParams, weight, fetcher, cacheName);
   }

}
