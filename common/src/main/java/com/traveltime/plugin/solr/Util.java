package com.traveltime.plugin.solr;

import com.traveltime.sdk.dto.requests.proto.Country;
import com.traveltime.sdk.dto.requests.proto.RequestType;
import com.traveltime.sdk.dto.requests.proto.Transportation;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.val;
import org.slf4j.Logger;

public final class Util {
  private Util() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  public static <T> T findByNameOrError(
      String what, String name, Function<String, Optional<T>> finder) {
    val result = finder.apply(name);
    if (!result.isPresent()) {
      throw new IllegalArgumentException(
          String.format("Couldn't find a %s with the name %s", what, name));
    } else {
      return result.get();
    }
  }

  public static Optional<Transportation> findModeByName(String name) {
    return Arrays.stream(Transportation.values())
        .filter(it -> it.getValue().equals(name))
        .findFirst();
  }

  public static Optional<Country> findCountryByName(String name) {
    return Arrays.stream(Country.values()).filter(it -> it.getValue().equals(name)).findFirst();
  }

  public static Optional<RequestType> findRequestTypeByName(String name) {
    return Arrays.stream(RequestType.values()).filter(it -> it.toString().equals(name)).findFirst();
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
