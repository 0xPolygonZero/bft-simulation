import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Holds some static utility methods. */
class Util {
  static <K> Set<K> keysWithMinCount(Map<K, Integer> counts, int min) {
    return counts.keySet().stream()
        .filter(k -> counts.get(k) >= min)
        .collect(Collectors.toSet());
  }
}
