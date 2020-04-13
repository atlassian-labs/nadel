package graphql.nadel.result;

import graphql.Assert;
import graphql.GraphQLError;
import graphql.Internal;
import graphql.execution.ExecutionPath;
import graphql.execution.NonNullableFieldWasNullException;
import graphql.execution.nextgen.result.ResolvedValue;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static graphql.Assert.assertNotNull;

@Internal
public abstract class ExecutionResultNode {

    private final ResolvedValue resolvedValue;
    private final NonNullableFieldWasNullException nonNullableFieldWasNullException;
    private final List<ExecutionResultNode> children;
    private final List<GraphQLError> errors;
    private final ElapsedTime elapsedTime;
    private final int totalNodeCount;

    private final ExecutionPath executionPath;

    private final String alias;
    private final List<String> fieldIds;

    private final GraphQLFieldDefinition fieldDefinition;
    private final GraphQLObjectType objectType;


    /*
     * we are trusting here the the children list is not modified on the outside (no defensive copy)
     */
    protected ExecutionResultNode(BuilderBase builderBase) {
        this.resolvedValue = builderBase.resolvedValue;
        this.children = Collections.unmodifiableList(assertNotNull(builderBase.children));
        children.forEach(Assert::assertNotNull);
        this.errors = Collections.unmodifiableList(builderBase.errors);
        this.elapsedTime = builderBase.elapsedTime;
        this.totalNodeCount = builderBase.totalNodeCount;
        this.executionPath = assertNotNull(builderBase.executionPath);

        this.alias = builderBase.alias;
        this.fieldIds = builderBase.fieldIds;
        this.fieldDefinition = builderBase.fieldDefinition;
        this.objectType = builderBase.objectType;

        if (builderBase.nonNullableFieldWasNullException == null) {
            this.nonNullableFieldWasNullException = ResultNodesUtil.newNullableException(fieldDefinition, getChildren());
        } else {
            this.nonNullableFieldWasNullException = builderBase.nonNullableFieldWasNullException;
        }
    }

    public ElapsedTime getElapsedTime() {
        return elapsedTime;
    }

    public List<GraphQLError> getErrors() {
        return errors;
    }

    /*
     * can be null for the RootExecutionResultNode
     */
    public ResolvedValue getResolvedValue() {
        return resolvedValue;
    }

    //    public MergedField getMergedField() {
//        return field;
//    }
    public String getResultKey() {
        return alias != null ? alias : fieldDefinition.getName();
    }

    public String getAlias() {
        return alias;
    }

    public List<String> getFieldIds() {
        return fieldIds;
    }

    public String getFieldName() {
        return fieldDefinition.getName();
    }

//    public MergedField getField() {
//        return field;
//    }

    public GraphQLFieldDefinition getFieldDefinition() {
        return fieldDefinition;
    }

    public GraphQLObjectType getObjectType() {
        return objectType;
    }

    public NonNullableFieldWasNullException getNonNullableFieldWasNullException() {
        return nonNullableFieldWasNullException;
    }

    public List<ExecutionResultNode> getChildren() {
        return this.children;
    }

    public Optional<NonNullableFieldWasNullException> getChildNonNullableException() {
        return children.stream()
                .filter(executionResultNode -> executionResultNode.getNonNullableFieldWasNullException() != null)
                .map(ExecutionResultNode::getNonNullableFieldWasNullException)
                .findFirst();
    }

    public Object getValue() {
        return getResolvedValue().getCompletedValue();
    }

    public int getTotalNodeCount() {
        return totalNodeCount;
    }


    public ExecutionPath getExecutionPath() {
        return executionPath;
    }

    public ExecutionResultNode withNewChildren(List<ExecutionResultNode> children) {
        return transform(builder -> builder.children(children));
    }

    public ExecutionResultNode withNewResolvedValue(ResolvedValue resolvedValue) {
        return transform(builder -> builder.resolvedValue(resolvedValue));
    }

    public ExecutionResultNode withNewErrors(List<GraphQLError> errors) {
        return transform(builder -> builder.errors(errors));
    }

    public ExecutionResultNode withElapsedTime(ElapsedTime elapsedTime) {
        return transform(builder -> builder.elapsedTime(elapsedTime));
    }

    public abstract <B extends BuilderBase<B>> ExecutionResultNode transform(Consumer<B> builderConsumer);

    public ExecutionResultNode withNodeCount(int nodeCount) {
        return transform(builder -> builder.totalNodeCount(nodeCount));
    }


    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "path=" + executionPath +
                ", objectType=" + (objectType != null ? objectType.getName() : "null") +
                ", name=" + (fieldDefinition != null ? fieldDefinition.getName() : "null") +
                ", fieldDefinition=" + fieldDefinition +
                ", resolvedValue=" + resolvedValue +
                ", nonNullableFieldWasNullException=" + nonNullableFieldWasNullException +
                ", children=" + children +
                ", errors=" + errors +
                '}';
    }

    public abstract static class BuilderBase<T extends BuilderBase<T>> {
        protected ResolvedValue resolvedValue;
        protected NonNullableFieldWasNullException nonNullableFieldWasNullException;
        protected List<ExecutionResultNode> children = new ArrayList<>();
        protected List<GraphQLError> errors = new ArrayList<>();
        protected ElapsedTime elapsedTime;
        protected ExecutionPath executionPath;

        private String alias;
        private List<String> fieldIds = new ArrayList<>();
        private GraphQLFieldDefinition fieldDefinition;
        private GraphQLObjectType objectType;
        private int totalNodeCount;


        public BuilderBase() {

        }

        public BuilderBase(ExecutionResultNode existing) {
            this.resolvedValue = existing.getResolvedValue();
            this.nonNullableFieldWasNullException = existing.getNonNullableFieldWasNullException();
            this.children.addAll(existing.getChildren());
            this.errors.addAll(existing.getErrors());
            this.elapsedTime = existing.getElapsedTime();
            this.executionPath = existing.getExecutionPath();
            this.alias = existing.getAlias();
            this.fieldIds.addAll(existing.getFieldIds());

            this.fieldDefinition = existing.fieldDefinition;
            this.objectType = existing.objectType;
            this.totalNodeCount = existing.totalNodeCount;
        }

        public abstract ExecutionResultNode build();

        public T resolvedValue(ResolvedValue resolvedValue) {
            this.resolvedValue = resolvedValue;
            return (T) this;
        }


        public T nonNullableFieldWasNullException(NonNullableFieldWasNullException nonNullableFieldWasNullException) {
            this.nonNullableFieldWasNullException = nonNullableFieldWasNullException;
            return (T) this;
        }

        public T objectType(GraphQLObjectType objectType) {
            this.objectType = objectType;
            return (T) this;
        }

        public T fieldDefinition(GraphQLFieldDefinition fieldDefinition) {
            this.fieldDefinition = fieldDefinition;
            return (T) this;
        }

        public T alias(String alias) {
            this.alias = alias;
            return (T) this;
        }

        public T fieldIds(List<String> fieldIds) {
            this.fieldIds.clear();
            this.fieldIds.addAll(fieldIds);
            return (T) this;
        }

        public T fieldId(String fieldId) {
            this.fieldIds.clear();
            this.fieldIds.add(fieldId);
            return (T) this;
        }

        public T children(List<ExecutionResultNode> children) {
            this.children.clear();
            this.children.addAll(children);
            return (T) this;
        }

        public T addChild(ExecutionResultNode child) {
            this.children.add(child);
            return (T) this;
        }

        public T errors(List<GraphQLError> errors) {
            this.errors.clear();
            this.errors = errors;
            return (T) this;
        }

        public T elapsedTime(ElapsedTime elapsedTime) {
            this.elapsedTime = elapsedTime;
            return (T) this;
        }

        public T executionPath(ExecutionPath executionPath) {
            this.executionPath = executionPath;
            return (T) this;
        }

        public T totalNodeCount(int totalNodeCount) {
            this.totalNodeCount = totalNodeCount;
            return (T) this;
        }

    }
}
