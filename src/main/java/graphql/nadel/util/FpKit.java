package graphql.nadel.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class FpKit {

    public static <T> Map<String, T> getByName(List<T> namedObjects, Function<T, String> nameFn) {
        return graphql.util.FpKit.getByName(namedObjects, nameFn);
    }

    public static <T, K, U> Collector<T, ?, Map<K, U>> toMapCollector(Function<? super T, ? extends K> keyMapper,
                                                                      Function<? super T, ? extends U> valueMapper) {
        return toMap(keyMapper, valueMapper, throwingMerger(), LinkedHashMap::new);
    }

    private static <T> BinaryOperator<T> throwingMerger() {
        return (u, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", u));
        };
    }

    @SafeVarargs
    public static <T> List<T> concat(List<T>... lists) {
        return Stream.of(lists).flatMap(Collection::stream).collect(toList());
    }
}
