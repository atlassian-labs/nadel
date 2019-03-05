package graphql.nadel;

import graphql.PublicApi;

import java.util.Map;

@PublicApi
public class ServiceExecutionResult {

    private final Map<String, Object> data;

    public ServiceExecutionResult(Map<String, Object> data) {
        this.data = data;
    }

    public Map<String, Object> getData() {
        return data;
    }

}
