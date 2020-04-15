package graphql.nadel.engine;

import graphql.GraphQLError;
import graphql.Internal;
import graphql.execution.ExecutionPath;
import graphql.execution.FetchedValue;
import graphql.nadel.normalized.NormalizedQueryField;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static graphql.Assert.assertNotNull;

@Internal
public class FetchedValueAnalysis {

    public enum FetchedValueType {
        OBJECT,
        LIST,
        SCALAR,
        ENUM,
        DEFER
    }

    private final FetchedValueType valueType;
    private final List<GraphQLError> errors;
    // not applicable for LIST
    private final Object completedValue;
    private final boolean nullValue;
    // only available for LIST
    private final List<FetchedValueAnalysis> children;
    // only for object
    private final GraphQLObjectType resolvedType;

    // for LIST this is the whole list
    private final FetchedValue fetchedValue;

    private final ExecutionPath executionPath;

    // this will change for elements inside lists. e.g.: [Foo!]! will change to Foo! inside the list
    private final GraphQLOutputType actualType;
    private final NormalizedQueryField normalizedQueryField;
    private final List<String> fieldIds;

    private FetchedValueAnalysis(Builder builder) {
        this.errors = new ArrayList<>(builder.errors);
        this.errors.addAll(builder.fetchedValue.getErrors());
        this.valueType = assertNotNull(builder.valueType);
        this.completedValue = builder.completedValue;
        this.nullValue = builder.nullValue;
        this.children = builder.children;
        this.resolvedType = builder.resolvedType;
        this.normalizedQueryField = assertNotNull(builder.normalizedQueryField);
        this.fetchedValue = assertNotNull(builder.fetchedValue);
        this.executionPath = assertNotNull(builder.executionPath);
        this.fieldIds = assertNotNull(builder.fieldIds);
        this.actualType = assertNotNull(builder.actualType);
    }

    public FetchedValueType getValueType() {
        return valueType;
    }

    public List<GraphQLError> getErrors() {
        return errors;
    }

    public Object getCompletedValue() {
        return completedValue;
    }

    public List<FetchedValueAnalysis> getChildren() {
        return children;
    }

    public boolean isNullValue() {
        return nullValue;
    }

    public FetchedValue getFetchedValue() {
        return fetchedValue;
    }

    public FetchedValueAnalysis transfrom(Consumer<Builder> builderConsumer) {
        Builder builder = new Builder(this);
        builderConsumer.accept(builder);
        return builder.build();
    }

    public static Builder newFetchedValueAnalysis() {
        return new Builder();
    }

    public static Builder newFetchedValueAnalysis(FetchedValueType valueType) {
        return new Builder().valueType(valueType);
    }

    public static Builder newFetchedValueAnalysis(FetchedValueAnalysis existing) {
        return new Builder(existing);
    }

    public GraphQLObjectType getResolvedType() {
        return resolvedType;
    }

    public String getResultKey() {
        return normalizedQueryField.getResultKey();
    }

    public ExecutionPath getExecutionPath() {
        return executionPath;
    }

    public NormalizedQueryField getNormalizedQueryField() {
        return normalizedQueryField;
    }

    public List<String> getFieldIds() {
        return fieldIds;
    }

    public GraphQLOutputType getActualType() {
        return actualType;
    }

    @Override
    public String toString() {
        return "{" +
                "valueType=" + valueType +
                ", completedValue=" + completedValue +
                ", errors=" + errors +
                ", children=" + children +
                ", nullValue=" + nullValue +
                ", resolvedType=" + resolvedType +
                ", fetchedValue=" + fetchedValue +
                '}';
    }

    public static final class Builder {
        private FetchedValueType valueType;
        private final List<GraphQLError> errors = new ArrayList<>();
        private Object completedValue;
        private FetchedValue fetchedValue;
        private List<FetchedValueAnalysis> children;
        private GraphQLObjectType resolvedType;
        private boolean nullValue;
        private NormalizedQueryField normalizedQueryField;
        private ExecutionPath executionPath;
        private List<String> fieldIds = new ArrayList<>();
        private GraphQLOutputType actualType;

        private Builder() {
        }

        private Builder(FetchedValueAnalysis existing) {
            valueType = existing.getValueType();
            errors.addAll(existing.getErrors());
            completedValue = existing.getCompletedValue();
            fetchedValue = existing.getFetchedValue();
            children = existing.getChildren();
            nullValue = existing.isNullValue();
            resolvedType = existing.getResolvedType();
            executionPath = existing.getExecutionPath();
            normalizedQueryField = existing.getNormalizedQueryField();
            fieldIds.addAll(existing.getFieldIds());
            actualType = existing.getActualType();
        }

        public Builder actualType(GraphQLOutputType graphQLOutputType) {
            this.actualType = graphQLOutputType;
            return this;
        }

        public Builder fieldIds(List<String> fieldIds) {
            this.fieldIds.clear();
            this.fieldIds.addAll(fieldIds);
            return this;
        }

        public Builder normalizedQueryField(NormalizedQueryField normalizedQueryField) {
            this.normalizedQueryField = normalizedQueryField;
            return this;
        }

        public Builder executionPath(ExecutionPath executionPath) {
            this.executionPath = executionPath;
            return this;
        }

        public Builder valueType(FetchedValueType val) {
            valueType = val;
            return this;
        }

        public Builder errors(List<GraphQLError> errors) {
            this.errors.addAll(errors);
            return this;
        }

        public Builder error(GraphQLError error) {
            this.errors.add(error);
            return this;
        }


        public Builder completedValue(Object completedValue) {
            this.completedValue = completedValue;
            return this;
        }

        public Builder children(List<FetchedValueAnalysis> children) {
            this.children = children;
            return this;
        }


        public Builder nullValue() {
            this.nullValue = true;
            return this;
        }

        public Builder resolvedType(GraphQLObjectType resolvedType) {
            this.resolvedType = resolvedType;
            return this;
        }


        public Builder fetchedValue(FetchedValue fetchedValue) {
            this.fetchedValue = fetchedValue;
            return this;
        }

        public FetchedValueAnalysis build() {
            return new FetchedValueAnalysis(this);
        }
    }
}
