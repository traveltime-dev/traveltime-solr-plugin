package com.traveltime.plugin.solr.cache;

import com.traveltime.sdk.dto.common.Coordinates;

/**
 * Encodes a Coordinates pair into a single long using the same quantization as Lucene's
 * GeoEncodingUtils. This is a lossless round-trip for coordinates decoded from Lucene's index
 * representation.
 */
final class CoordEncoding {
  private static final double LAT_SCALE = (0x1L << 32) / 180.0D;
  private static final double LON_SCALE = (0x1L << 32) / 360.0D;

  private CoordEncoding() {}

  static long encode(Coordinates coord) {
    int latBits = (int) Math.floor(coord.getLat() * LAT_SCALE);
    int lonBits = (int) Math.floor(coord.getLng() * LON_SCALE);
    return ((long) latBits << 32) | (lonBits & 0xFFFFFFFFL);
  }
}
