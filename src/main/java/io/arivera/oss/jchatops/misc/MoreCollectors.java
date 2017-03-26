package io.arivera.oss.jchatops.misc;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class MoreCollectors {

  public static <T, K, U> Collector<T, ?, Map<K,U>> toLinkedMap(
      Function<? super T, ? extends K> keyMapper,
      Function<? super T, ? extends U> valueMapper)
  {
    return Collectors.toMap(keyMapper, valueMapper,
        (u, v) -> {
          throw new IllegalStateException(String.format("Cannot create map with duplicate key: %s", u));
        },
        LinkedHashMap::new);
  }
}
