package graphql.nadel;

import graphql.PublicApi;

import java.util.Map;

@PublicApi
public class DelegatedExecutionResult {

    private final Map<String, Object> data;

    public DelegatedExecutionResult(Map<String, Object> data) {
        this.data = data;
    }

    public Map<String, Object> getData() {
        return data;
    }

}
