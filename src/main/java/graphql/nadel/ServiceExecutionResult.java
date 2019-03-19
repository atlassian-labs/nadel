package graphql.nadel;

import graphql.PublicApi;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@PublicApi
public class ServiceExecutionResult {
    private final List<Map<String, Object>> errors;

    private final Map<String, Object> data;

    public ServiceExecutionResult(Map<String, Object> data, List<Map<String, Object>> errors) {
        this.data = data;
        this.errors = errors;
    }

    public ServiceExecutionResult(Map<String, Object> data) {
        this.data = data;
        this.errors = Collections.emptyList();
    }

    public Map<String, Object> getData() {
        return data;
    }

    public List<Map<String, Object>> getErrors() {
        return errors;
    }
}
