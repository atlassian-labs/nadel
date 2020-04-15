package graphql.nadel.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
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

    public static <K, V> V getSingleMapValue(Map<K, V> map) {
        return map.values().iterator().next();
    }

    public static <T, U> List<U> filterAndMap(List<T> list, Predicate<T> filter, Function<T, U> function) {
        return list.stream().filter(filter).map(function).collect(Collectors.toList());
    }

    public static <T> List<T> filter(List<T> list, Predicate<T> filter) {
        return list.stream().filter(filter).collect(Collectors.toList());
    }

    public static <T, U> List<U> map(List<T> list, Function<T, U> function) {
        List<U> result = new ArrayList<>();
        for (T t : list) {
            result.add(function.apply(t));
        }
        return result;
    }

}
