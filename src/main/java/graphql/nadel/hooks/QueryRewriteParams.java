package graphql.nadel.hooks;

import graphql.PublicApi;
import graphql.execution.ExecutionId;
import graphql.execution.ExecutionStepInfo;
import graphql.language.Document;
import graphql.language.FragmentDefinition;
import graphql.nadel.Service;
import graphql.nadel.engine.NadelContext;
import graphql.schema.GraphQLObjectType;
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
    private final Map<String, FragmentDefinition> fragmentsByName;
    private final GraphQLObjectType operationRootType;


    private QueryRewriteParams(Builder builder) {
        this.service = builder.service;
        this.executionStepInfo = builder.executionStepInfo;
        this.executionId = builder.executionId;
        this.schema = builder.schema;
        this.nadelContext = builder.nadelContext;
        this.serviceContext = builder.serviceContext;
        this.document = builder.document;
        this.variables = builder.variables;
        this.fragmentsByName = builder.fragmentsByName;
        this.operationRootType = builder.operationRootType;
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

    public Map<String, FragmentDefinition> getFragmentsByName() {
        return fragmentsByName;
    }

    public GraphQLObjectType getOperationRootType() {
        return operationRootType;
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
        private Map<String, FragmentDefinition> fragmentsByName;
        private GraphQLObjectType operationRootType;

        public Builder from(QueryRewriteParams other) {
            this.service = other.service;
            this.executionStepInfo = other.executionStepInfo;
            this.executionId = other.executionId;
            this.schema = other.schema;
            this.nadelContext = other.nadelContext;
            this.document = other.document;
            this.variables = other.variables;
            this.fragmentsByName = other.fragmentsByName;
            this.operationRootType = other.operationRootType;
            return this;
        }

        public Builder executionId(ExecutionId executionId) {
            this.executionId = executionId;
            return this;
        }

        public Builder nadelContext(NadelContext nadelContext) {
            this.nadelContext = nadelContext;
            return this;
        }

        public Builder schema(GraphQLSchema schema) {
            this.schema = schema;
            return this;
        }

        public Builder fragmentsByName(Map<String, FragmentDefinition> fragmentsByName) {
            this.fragmentsByName = fragmentsByName;
            return this;
        }

        public Builder operationRootType(GraphQLObjectType operationRootType) {
            this.operationRootType = operationRootType;
            return this;
        }

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

        public Builder variables(Map<String, Object> variables) {
            this.variables = variables;
            return this;
        }

        public QueryRewriteParams build() {
            return new QueryRewriteParams(this);
        }
    }
}
