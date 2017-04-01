package io.arivera.oss.jchatops.misc;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class MoreCollectors {

  /**
   * Collects into a LinkedMap
   *
   * @throws IllegalStateException when a duplicated key is found.
   */
  public static <T, K, U> Collector<T, ?, Map<K, U>> toLinkedMap(
      Function<? super T, ? extends K> keyMapper,
      Function<? super T, ? extends U> valueMapper) {

    return Collectors.toMap(keyMapper, valueMapper,
        (key, value) -> {
          throw new IllegalStateException(String.format("Cannot create map with duplicate key: %s", key));
        },
        LinkedHashMap::new);
  }
}
