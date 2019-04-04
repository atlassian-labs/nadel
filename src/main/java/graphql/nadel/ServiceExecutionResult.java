package graphql.nadel;

import graphql.PublicApi;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

@PublicApi
public class ServiceExecutionResult {
    private final List<Map<String, Object>> errors;

    private final Map<String, Object> data;

    public ServiceExecutionResult(Map<String, Object> data, List<Map<String, Object>> errors) {
        this.data = data;
        this.errors = errors == null ? emptyList() : errors;
    }

    public ServiceExecutionResult(Map<String, Object> data) {
        this.data = data;
        this.errors = emptyList();
    }

    public Map<String, Object> getData() {
        return data;
    }

    public List<Map<String, Object>> getErrors() {
        return errors;
    }
}
