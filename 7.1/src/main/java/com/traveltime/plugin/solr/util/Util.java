package com.traveltime.plugin.solr.util;

import com.traveltime.sdk.dto.common.Coordinates;
import java.util.Optional;
import java.util.function.Function;
import lombok.val;
import org.apache.lucene.geo.GeoEncodingUtils;
import org.apache.lucene.geo.GeoUtils;
import org.apache.solr.common.SolrException;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.LatLonPointSpatialField;
import org.apache.solr.search.SyntaxError;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.io.GeohashUtils;

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
          SolrException.ErrorCode.BAD_REQUEST, "Could not decode string" + str + " as coordinates");
    }
  }

  public static Function<String, Optional<SyntaxError>> fieldValidator(IndexSchema schema) {
    return field -> {
      if (!(schema.getField(field).getType() instanceof LatLonPointSpatialField)) {
        return Optional.of(
            new SyntaxError("field[" + field + "] is not a LatLonPointSpatialField"));
      } else {
        return Optional.empty();
      }
    };
  }
}
