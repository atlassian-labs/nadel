package graphql.nadel.util;

import graphql.Internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Internal
public class FpKit {

    public static <T> Map<String, T> getByName(List<T> namedObjects, Function<T, String> nameFn) {
        Map<String, T> result = new LinkedHashMap<>();
        for (T t : namedObjects) {
            result.put(nameFn.apply(t), t);
        }
        return result;
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

    public static <T, U> List<U> filterAndMap(Collection<T> collection, Predicate<T> filter, Function<T, U> function) {
        List<U> result = new ArrayList<>();
        for (T t : collection) {
            if (filter.test(t)) {
                result.add(function.apply(t));
            }
        }
        return result;
    }

    public static <T> List<T> filter(Collection<T> collection, Predicate<T> filter) {
        List<T> result = new ArrayList<>();
        for (T t : collection) {
            if (filter.test(t)) {
                result.add(t);
            }
        }
        return result;
    }

    public static <T, U> List<U> map(List<T> list, Function<T, U> function) {
        List<U> result = new ArrayList<>();
        for (T t : list) {
            result.add(function.apply(t));
        }
        return result;
    }

    public static <T, U> List<U> flatMap(List<T> list, Function<T, List<U>> function) {
        List<U> result = new ArrayList<>();
        for (T t : list) {
            result.addAll(function.apply(t));
        }
        return result;
    }

    public static <T, U> List<U> mapAndFilter(List<T> list, Function<T, U> function, Predicate<U> filter) {
        List<U> result = new ArrayList<>(list.size());
        for (T t : list) {
            U mappedValue = function.apply(t);
            if (filter.test(mappedValue)) {
                result.add(mappedValue);
            }
        }
        return result;
    }

    public static <T> Optional<T> findOne(Collection<T> collection, Predicate<T> filter) {
        for (T t : collection) {
            if (filter.test(t)) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    public static <T> T findOneOrNull(List<T> list, Predicate<T> filter) {
        return findOne(list, filter).orElse(null);
    }

    public static <T> List<T> flatList(List<List<T>> listLists) {
        List<T> result = new ArrayList<>();
        for (List<T> list : listLists) {
            result.addAll(list);
        }
        return result;
    }


}
