package graphql.nadel;

import graphql.PublicApi;

import java.util.LinkedHashMap;
import java.util.Map;

import static graphql.GraphQLContext.newContext;

@PublicApi
public class NadelExecutionInput {

    private final String query;
    private final String operationName;
    private final Object context;
    private final Map<String, Object> variables;

    private NadelExecutionInput(String query, String operationName, Object context, Map<String, Object> variables) {
        this.query = query;
        this.operationName = operationName;
        this.context = context;
        this.variables = variables;
    }

    public String getQuery() {
        return query;
    }

    public String getOperationName() {
        return operationName;
    }

    public Object getContext() {
        return context;
    }

    public Map<String, Object> getVariables() {
        return new LinkedHashMap<>(variables);
    }

    public static Builder newNadelExecutionInput() {
        return new Builder();
    }

    public static class Builder {
        private String query;
        private String operationName;
        private Object context = newContext().build();
        private Map<String, Object> variables = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder query(String query) {
            this.query = query;
            return this;
        }

        public Builder operationName(String operationName) {
            this.operationName = operationName;
            return this;
        }

        public Builder context(Object context) {
            this.context = context;
            return this;
        }

        public Builder variables(Map<String, Object> variables) {
            this.variables = variables;
            return this;
        }

        public NadelExecutionInput build() {
            return new NadelExecutionInput(query, operationName, context, variables);
        }

    }
}
