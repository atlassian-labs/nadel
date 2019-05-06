package graphql.nadel;

import graphql.PublicApi;

import java.util.LinkedHashMap;
import java.util.Map;

import static graphql.GraphQLContext.newContext;
import static java.util.Objects.requireNonNull;

@PublicApi
public class NadelExecutionInput {

    private final String query;
    private final String operationName;
    private final Object context;
    private final Map<String, Object> variables;
    private final String artificialFieldsUUID;

    private NadelExecutionInput(String query, String operationName, Object context, Map<String, Object> variables, String artificialFieldsUUID) {
        this.query = requireNonNull(query);
        this.operationName = operationName;
        this.context = context;
        this.variables = requireNonNull(variables);
        this.artificialFieldsUUID = artificialFieldsUUID;
    }

    public String getQuery() {
        return query;
    }

    public String getArtificialFieldsUUID() {
        return artificialFieldsUUID;
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
        private String artificialFieldsUUID;

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

        public Builder artificialFieldsUUID(String artificialFieldsUUID) {
            this.artificialFieldsUUID = artificialFieldsUUID;
            return this;
        }

        public NadelExecutionInput build() {
            return new NadelExecutionInput(query, operationName, context, variables, artificialFieldsUUID);
        }

    }
}
