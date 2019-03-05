package graphql.nadel;

import graphql.PublicApi;
import graphql.language.Document;
import graphql.language.FragmentDefinition;
import graphql.language.OperationDefinition;

import java.util.LinkedHashMap;
import java.util.Map;

import static graphql.Assert.assertNotNull;


@PublicApi
public class ServiceExecutionParameters {

    private final Document query;
    private final Object context;
    private final Map<String, Object> variables;
    private final Map<String, FragmentDefinition> fragments;
    private final OperationDefinition operationDefinition;

    private ServiceExecutionParameters(Document query, Object context, Map<String, Object> variables, Map<String, FragmentDefinition> fragments, OperationDefinition operationDefinition) {
        this.query = assertNotNull(query);
        this.variables = assertNotNull(variables);
        this.fragments = assertNotNull(fragments);
        this.operationDefinition = assertNotNull(operationDefinition);
        this.context = context;
    }

    public Document getQuery() {
        return query;
    }

    public Object getContext() {
        return context;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public Map<String, FragmentDefinition> getFragments() {
        return fragments;
    }

    public OperationDefinition getOperationDefinition() {
        return operationDefinition;
    }

    public OperationDefinition.Operation getOperation() {
        return operationDefinition.getOperation();
    }


    public static Builder newServiceExecutionParameters() {
        return new Builder();
    }

    public static class Builder {
        private Document query;
        private Object context;
        private Map<String, Object> variables = new LinkedHashMap<>();
        private Map<String, FragmentDefinition> fragments = new LinkedHashMap<>();
        private OperationDefinition operationDefinition;

        private Builder() {
        }

        public Builder query(Document query) {
            this.query = query;
            return this;
        }

        public Builder context(Object context) {
            this.context = context;
            return this;
        }

        public Builder variables(Map<String, Object> variables) {
            this.variables.putAll(variables);
            return this;
        }

        public Builder fragments(Map<String, FragmentDefinition> fragments) {
            this.fragments.putAll(fragments);
            return this;
        }

        public Builder operationDefinition(OperationDefinition operationDefinition) {
            this.operationDefinition = operationDefinition;
            return this;
        }

        public ServiceExecutionParameters build() {
            return new ServiceExecutionParameters(query, context, variables, fragments, operationDefinition);
        }
    }
}
