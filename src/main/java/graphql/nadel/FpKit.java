package graphql.nadel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class FpKit {

    public static <T> Map<String, T> getByName(List<T> namedObjects, Function<T, String> nameFn) {
        return graphql.util.FpKit.getByName(namedObjects, nameFn);
    }

    public static <T, K, U> Collector<T, ?, Map<K, U>> toMapCollector(Function<? super T, ? extends K> keyMapper,
                                                                      Function<? super T, ? extends U> valueMapper) {
        return Collectors.toMap(keyMapper, valueMapper, throwingMerger(), LinkedHashMap::new);
    }

    private static <T> BinaryOperator<T> throwingMerger() {
        return (u, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", u));
        };
    }
}
