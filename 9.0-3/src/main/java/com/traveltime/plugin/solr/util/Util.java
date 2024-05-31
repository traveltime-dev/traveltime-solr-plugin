package com.traveltime.plugin.solr.util;

import com.traveltime.sdk.dto.common.Coordinates;
import com.traveltime.sdk.dto.requests.proto.Country;
import com.traveltime.sdk.dto.requests.proto.Transportation;
import lombok.val;
import org.apache.lucene.geo.GeoEncodingUtils;
import org.apache.lucene.geo.GeoUtils;
import org.apache.solr.common.SolrException;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.io.GeohashUtils;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

public final class Util {
   private Util() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static Coordinates decode(long value) {
      double lat = GeoEncodingUtils.decodeLatitude((int) (value >> 32));
      double lon = GeoEncodingUtils.decodeLongitude((int) value);
      return new Coordinates(lat, lon);
   }

   public static Coordinates toGeoPoint(String str) {
      String[] coords = str.split(",");
      Double lat = null;
      Double lng = null;

      if (coords.length == 2) {
         try {
            lat = Double.parseDouble(coords[0]);
            lng = Double.parseDouble(coords[1]);
         } catch (NumberFormatException ignored) {
         }
      } else {
         try {
            val point = GeohashUtils.decode(str, SpatialContext.GEO);
            lat = point.getY();
            lng = point.getX();
         } catch (ArrayIndexOutOfBoundsException ignored) {
         }
      }

      if (lat != null && lng != null) {
         GeoUtils.checkLatitude(lat);
         GeoUtils.checkLongitude(lat);
         return new Coordinates(lat, lng);
      } else {
         throw new SolrException(
               SolrException.ErrorCode.BAD_REQUEST,
               "Could not decode string" + str + " as coordinates"
         );
      }
   }

   public static Optional<Transportation> findModeByName(String name) {
      return Arrays.stream(Transportation.values()).filter(it -> it.getValue().equals(name)).findFirst();
   }

   public static Optional<Country> findCountryByName(String name) {
      return Arrays.stream(Country.values()).filter(it -> it.getValue().equals(name)).findFirst();
   }

   public static <A> A time(Logger logger, Supplier<A> expr) {
      val startTime = System.currentTimeMillis();
      val res = expr.get();
      val endTime = System.currentTimeMillis();
      val lastStack = Thread.currentThread().getStackTrace()[2].toString();
      val message = String.format("In %s took %d ms", lastStack, endTime - startTime);
      logger.info(message);
      return res;
   }
}
