package graphql.nadel.hooks;

import graphql.Internal;
import graphql.PublicApi;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionId;
import graphql.execution.ExecutionStepInfo;
import graphql.language.Document;
import graphql.nadel.Service;
import graphql.nadel.engine.NadelContext;
import graphql.schema.GraphQLSchema;

import java.util.Map;

@PublicApi
public class QueryRewriteParams {
    private final Service service;
    private final ExecutionStepInfo executionStepInfo;
    private final ExecutionId executionId;
    private final GraphQLSchema schema;
    private final NadelContext nadelContext;
    private final Object serviceContext;
    private final Document document;
    private final Map<String, Object> variables;


    private QueryRewriteParams(Builder builder) {
        this.service = builder.service;
        this.executionStepInfo = builder.executionStepInfo;
        this.executionId = builder.executionId;
        this.schema = builder.schema;
        this.nadelContext = builder.nadelContext;
        this.serviceContext = builder.serviceContext;
        this.document = builder.document;
        this.variables = builder.variables;
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

    public Document getDocument() {
        return document;
    }

    public Map<String, Object> getVariables() {
        return variables;
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
        private Document document;
        private Map<String, Object> variables;

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

        public Builder document(Document document) {
            this.document = document;
            return this;
        }

        @Internal
        public Builder executionContext(ExecutionContext executionContext) {
            this.executionId = executionContext.getExecutionId();
            this.schema = executionContext.getGraphQLSchema();
            this.nadelContext = (NadelContext) executionContext.getContext();
            return this;
        }

        public Builder variables(Map<String, Object> variables) {
            this.variables = variables;
            return this;
        }

        public QueryRewriteParams build() {
            return new QueryRewriteParams(this);
        }
    }
}
