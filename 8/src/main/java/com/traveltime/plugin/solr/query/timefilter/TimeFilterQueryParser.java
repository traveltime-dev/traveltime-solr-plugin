package com.traveltime.plugin.solr.query.timefilter;

import com.traveltime.plugin.solr.fetcher.JsonFetcher;
import com.traveltime.plugin.solr.util.Util;
import lombok.val;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.SyntaxError;

import java.util.Optional;

public class TimeFilterQueryParser extends QParser {
   private static final String WEIGHT = "weight";

   private final JsonFetcher fetcher;
   private final String cacheName;

   public TimeFilterQueryParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req, JsonFetcher fetcher, String cacheName) {
      super(qstr, localParams, params, req);
      this.fetcher = fetcher;
      this.cacheName = cacheName;
   }

   private String getBestParam(String name) throws SyntaxError {
      String param = Util.getBestParam(localParams, params, name);
      Util.assertPresent(param, name);
      return param;
   }

   private Optional<String> getOptionalBestParam(String name) {
      String param = Util.getBestParam(localParams, params, name);
      return Optional.ofNullable(param);
   }

   @Override
   public TimeFilterSearchQuery parse() throws SyntaxError {
      float weight;
      try {
         weight = Float.parseFloat(getBestParam(WEIGHT));
      } catch (SyntaxError ignored) {
         weight = 0f;
      } catch (NumberFormatException e) {
         throw new SyntaxError("Couldn't parse traveltime weight as a float");
      }
      if(weight < 0 || weight > 1) {
         throw new SyntaxError("Traveltime weight must be between 0 and 1");
      }

      val arrivalTime = getOptionalBestParam(TimeFilterQueryParameters.ARRIVAL_TIME);
      val arrivalLocation = getOptionalBestParam(TimeFilterQueryParameters.ARRIVAL_LOCATION);

      val departureTime = getOptionalBestParam(TimeFilterQueryParameters.DEPARTURE_TIME);
      val departureLocation = getOptionalBestParam(TimeFilterQueryParameters.DEPARTURE_LOCATION);

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
                 getBestParam(TimeFilterQueryParameters.FIELD),
                 getBestParam(TimeFilterQueryParameters.ARRIVAL_LOCATION),
                 getBestParam(TimeFilterQueryParameters.ARRIVAL_TIME),
                 getBestParam(TimeFilterQueryParameters.TRAVEL_TIME),
                 getBestParam(TimeFilterQueryParameters.TRANSPORTATION),
                 getOptionalBestParam(TimeFilterQueryParameters.RANGE)
         );
      } else {
         queryParams = TimeFilterQueryParameters.fromStrings(
                 req.getSchema(),
                 TimeFilterQueryParameters.SearchType.DEPARTURE,
                 getBestParam(TimeFilterQueryParameters.FIELD),
                 getBestParam(TimeFilterQueryParameters.DEPARTURE_LOCATION),
                 getBestParam(TimeFilterQueryParameters.DEPARTURE_TIME),
                 getBestParam(TimeFilterQueryParameters.TRAVEL_TIME),
                 getBestParam(TimeFilterQueryParameters.TRANSPORTATION),
                 getOptionalBestParam(TimeFilterQueryParameters.RANGE)
         );
      }

      return new TimeFilterSearchQuery(queryParams, weight, fetcher, cacheName);
   }

}
