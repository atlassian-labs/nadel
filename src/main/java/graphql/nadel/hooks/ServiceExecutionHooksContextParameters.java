package graphql.nadel.hooks;

import graphql.Internal;
import graphql.PublicApi;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionId;
import graphql.execution.ExecutionStepInfo;
import graphql.language.FragmentDefinition;
import graphql.nadel.Service;
import graphql.nadel.engine.NadelContext;
import graphql.schema.GraphQLSchema;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;

@PublicApi
public class ServiceExecutionHooksContextParameters {
    private final Service service;
    private final ExecutionStepInfo executionStepInfo;
    private final ExecutionId executionId;
    private final GraphQLSchema schema;
    private final Map<String, FragmentDefinition> fragmentsByName;
    private final Map<String, Object> variables;
    private final NadelContext context;


    private ServiceExecutionHooksContextParameters(Builder builder) {
        this.service = builder.service;
        this.executionStepInfo = builder.executionStepInfo;
        this.executionId = builder.executionId;
        this.schema = builder.schema;
        this.fragmentsByName = unmodifiableMap(new LinkedHashMap<>(builder.fragmentsByName));
        this.variables = unmodifiableMap(new LinkedHashMap<>(builder.variables));
        this.context = builder.context;
    }

    public Service getService() {
        return service;
    }

    public ExecutionStepInfo getExecutionStepInfo() {
        return executionStepInfo;
    }

    public ExecutionId getExecutionId() {
        return executionId;
    }

    public GraphQLSchema getSchema() {
        return schema;
    }

    public Map<String, FragmentDefinition> getFragmentsByName() {
        return fragmentsByName;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public NadelContext getContext() {
        return context;
    }

    public static Builder newContextParameters() {
        return new Builder();
    }

    public static class Builder {
        private Service service;
        private ExecutionStepInfo executionStepInfo;
        private ExecutionId executionId;
        private GraphQLSchema schema;
        private Map<String, FragmentDefinition> fragmentsByName;
        private Map<String, Object> variables;
        private NadelContext context;

        public Builder service(Service service) {
            this.service = service;
            return this;
        }

        public Builder executionStepInfo(ExecutionStepInfo executionStepInfo) {
            this.executionStepInfo = executionStepInfo;
            return this;
        }

        @Internal
        public Builder executionContext(ExecutionContext executionContext) {
            this.executionId = executionContext.getExecutionId();
            this.schema = executionContext.getGraphQLSchema();
            this.fragmentsByName = executionContext.getFragmentsByName();
            this.variables = executionContext.getVariables();
            this.context = (NadelContext) executionContext.getContext();
            return this;
        }

        public ServiceExecutionHooksContextParameters build() {
            return new ServiceExecutionHooksContextParameters(this);
        }
    }
}
