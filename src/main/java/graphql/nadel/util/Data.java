package graphql.nadel.util;

import graphql.Internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static graphql.Assert.assertNotNull;
import static graphql.Assert.assertTrue;

/**
 * This class will probably send James Gosling and Joshua Bloch into apoplexy
 * but god damn it can we please have data classes in Java.
 * <p>
 * Do NOT leak out this class in any API related manner.
 * <p>
 * Its especially useful for CompletableFutures where you can only
 * give back one value.  It saves you creating a full POJO with getters
 * and builder and so on just to hold more than one value.
 * <p>
 * At its heart, its a glorified map untyped map trying to act like
 * a POJO but its addicted to heroin.
 * <p>
 * Remember : Too much syntactical sugar gives you cancer of the colon!
 */
@Internal
@SuppressWarnings("unchecked")
public class Data {

    private final Map<String, Object> map;

    private Data(Builder builder) {
        this.map = new HashMap<>(builder.map);
    }

    @Override
    public String toString() {
        return map.toString();
    }

    public <T> T get(Class<? extends T> key) {
        return getOrDefault(key, null);
    }

    public <T> T get(String key) {
        return getOrDefault(key, null);
    }

    public <T> T getOrDefault(String key, Object defaultValue) {
        return (T) map.getOrDefault(assertNotNull(key), defaultValue);
    }

    public <T> T getOrDefault(Class<? extends T> key, Object defaultValue) {
        T val = getOrDefault(assertNotNull(key).getCanonicalName(), defaultValue);
        assertTrue(isOfClass(val, key), "Expecting a value of class '%s'", key.getCanonicalName());
        return val;
    }

    private <T> boolean isOfClass(T val, Class<? extends T> key) {
        if (val == null) {
            return true;
        }
        return key.isAssignableFrom(val.getClass());
    }

    public Map<String, Object> asMap() {
        return new HashMap<>(map);
    }

    public static Data of(Object v1) {
        return newData()
                .set(assertNotNull(v1).getClass(), v1)
                .build();
    }

    public static Data of(Object... values) {
        Builder builder = newData();
        for (Object value : values) {
            assertNotNull(value);
            builder.set(value.getClass(), value);
        }
        return builder.build();
    }

    public static Builder newData(Object v1) {
        return newData().set(v1.getClass(), v1);
    }

    public static Builder newData(Object... values) {
        Builder builder = newData();
        for (Object value : values) {
            assertNotNull(value);
            builder.set(value.getClass(), value);
        }
        return builder;
    }

    public static Builder newData() {
        return new Builder();
    }

    public static class Builder {
        private final Map<String, Object> map = new HashMap<>();

        public Builder set(Class<?> key, Object value) {
            key = remapKey(assertNotNull(key));
            map.put(assertNotNull(key).getCanonicalName(), value);
            return this;
        }

        private Class<?> remapKey(Class<?> key) {
            if (List.class.isAssignableFrom(key)) {
                return List.class;
            }
            if (Set.class.isAssignableFrom(key)) {
                return Set.class;
            }
            if (Map.class.isAssignableFrom(key)) {
                return Map.class;
            }
            if (Queue.class.isAssignableFrom(key)) {
                return Queue.class;
            }
            if (Collection.class.isAssignableFrom(key)) {
                return Collection.class;
            }
            return key;
        }

        public Builder set(String key, Object value) {
            map.put(assertNotNull(key), value);
            return this;
        }

        public Data build() {
            return new Data(this);
        }
    }
}