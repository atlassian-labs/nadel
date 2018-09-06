package graphql.nadel;

import graphql.schema.DataFetcher;

import java.util.Map;

/**
 * Creates data fetcher based on name.
 */
public interface DataFetcherFactory {
    DataFetcherFactory DEFAULT = name -> null;

    static DataFetcherFactory fromMap(Map<String, DataFetcher<Object>> fetchers) {
        return fetchers::get;
    }

    /**
     * Creates data fetcher by name.
     *
     * @param name of the data fetcher that is needed
     *
     * @return instance of the data fetcher for the given name or null if data fetcher does not exist.
     */
    DataFetcher<Object> createDataFetcher(String name);
}
