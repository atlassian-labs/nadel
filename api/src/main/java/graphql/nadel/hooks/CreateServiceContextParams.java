package graphql.nadel.hooks;

import graphql.Internal;
import graphql.PublicApi;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionId;
import graphql.execution.ExecutionStepInfo;
import graphql.nadel.Service;
import graphql.nadel.engine.NadelContext;
import graphql.schema.GraphQLSchema;

@PublicApi
public class CreateServiceContextParams {
    private final Service service;
    private final ExecutionStepInfo executionStepInfo;
    private final ExecutionId executionId;
    private final GraphQLSchema schema;
    private final NadelContext context;

    private CreateServiceContextParams(Builder builder) {
        this.service = builder.service;
        this.executionStepInfo = builder.executionStepInfo;
        this.executionId = builder.executionId;
        this.schema = builder.schema;
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

    public NadelContext getContext() {
        return context;
    }

    public static Builder newParameters() {
        return new Builder();
    }

    public static class Builder {
        private Service service;
        private ExecutionStepInfo executionStepInfo;
        private ExecutionId executionId;
        private GraphQLSchema schema;
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
        public Builder from(ExecutionContext executionContext) {
            this.executionId = executionContext.getExecutionId();
            this.schema = executionContext.getGraphQLSchema();
            this.context = (NadelContext) executionContext.getContext();
            return this;
        }

        public CreateServiceContextParams build() {
            return new CreateServiceContextParams(this);
        }
    }
}
