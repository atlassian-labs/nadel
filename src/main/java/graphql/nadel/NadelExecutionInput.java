package graphql.nadel;

import graphql.PublicApi;

import java.util.Map;

@PublicApi
public class NadelExecutionInput {

    private final String query;
    private final String operationName;
    private final Map<String, Object> variables;

    public NadelExecutionInput(String query, String operationName, Map<String, Object> variables) {
        this.query = query;
        this.operationName = operationName;
        this.variables = variables;
    }
}
