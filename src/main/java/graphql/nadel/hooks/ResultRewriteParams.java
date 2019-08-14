package graphql.nadel.hooks;

import graphql.Internal;
import graphql.PublicApi;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionId;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.nextgen.result.RootExecutionResultNode;
import graphql.nadel.Service;
import graphql.nadel.engine.NadelContext;
import graphql.schema.GraphQLSchema;

@PublicApi
public class ResultRewriteParams {
    private final Service service;
    private final ExecutionStepInfo executionStepInfo;
    private final ExecutionId executionId;
    private final GraphQLSchema schema;
    private final NadelContext nadelContext;
    private final Object serviceContext;
    private final RootExecutionResultNode resultNode;


    private ResultRewriteParams(Builder builder) {
        this.service = builder.service;
        this.executionStepInfo = builder.executionStepInfo;
        this.executionId = builder.executionId;
        this.schema = builder.schema;
        this.nadelContext = builder.nadelContext;
        this.serviceContext = builder.serviceContext;
        this.resultNode = builder.resultNode;
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

    public NadelContext getNadelContext() {
        return nadelContext;
    }

    public Object getServiceContext() {
        return serviceContext;
    }

    public RootExecutionResultNode getResultNode() {
        return resultNode;
    }

    public static Builder newParameters() {
        return new Builder();
    }

    public static class Builder {
        private Service service;
        private ExecutionStepInfo executionStepInfo;
        private ExecutionId executionId;
        private GraphQLSchema schema;
        private Object serviceContext;
        private NadelContext nadelContext;
        private RootExecutionResultNode resultNode;

        public Builder service(Service service) {
            this.service = service;
            return this;
        }

        public Builder executionStepInfo(ExecutionStepInfo executionStepInfo) {
            this.executionStepInfo = executionStepInfo;
            return this;
        }

        public Builder serviceContext(Object serviceContext) {
            this.serviceContext = serviceContext;
            return this;
        }

        public Builder resultNode(RootExecutionResultNode resultNode) {
            this.resultNode = resultNode;
            return this;
        }

        @Internal
        public Builder from(ExecutionContext executionContext) {
            this.executionId = executionContext.getExecutionId();
            this.schema = executionContext.getGraphQLSchema();
            this.nadelContext = (NadelContext) executionContext.getContext();
            return this;
        }

        public ResultRewriteParams build() {
            return new ResultRewriteParams(this);
        }
    }
}
