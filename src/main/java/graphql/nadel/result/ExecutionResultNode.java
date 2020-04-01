package graphql.nadel.result;

import graphql.Assert;
import graphql.GraphQLError;
import graphql.Internal;
import graphql.execution.ExecutionPath;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.execution.NonNullableFieldWasNullException;
import graphql.execution.nextgen.result.ResolvedValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static graphql.Assert.assertNotNull;

@Internal
public abstract class ExecutionResultNode {

    private final ExecutionStepInfo executionStepInfo;
    private final ResolvedValue resolvedValue;
    private final NonNullableFieldWasNullException nonNullableFieldWasNullException;
    private final List<ExecutionResultNode> children;
    private final List<GraphQLError> errors;
    private final ElapsedTime elapsedTime;
    private final ExecutionPath executionPath;

    /*
     * we are trusting here the the children list is not modified on the outside (no defensive copy)
     */
    protected ExecutionResultNode(BuilderBase builderBase) {
        this.resolvedValue = builderBase.resolvedValue;
        this.executionStepInfo = builderBase.executionStepInfo;
        this.children = Collections.unmodifiableList(assertNotNull(builderBase.children));
        children.forEach(Assert::assertNotNull);
        this.errors = Collections.unmodifiableList(builderBase.errors);
        this.elapsedTime = builderBase.elapsedTime;
        this.executionPath = Assert.assertNotNull(builderBase.executionPath);

        if (builderBase.nonNullableFieldWasNullException == null) {
            this.nonNullableFieldWasNullException = ResultNodesUtil.newNullableException(executionStepInfo, getChildren());
        } else {
            this.nonNullableFieldWasNullException = builderBase.nonNullableFieldWasNullException;
        }
    }

    public ElapsedTime getElapsedTime() {
        return elapsedTime;
    }

    public List<GraphQLError> getErrors() {
        return new ArrayList<>(errors);
    }

    /*
     * can be null for the RootExecutionResultNode
     */
    public ResolvedValue getResolvedValue() {
        return resolvedValue;
    }

    public MergedField getMergedField() {
        return executionStepInfo.getField();
    }

    public ExecutionStepInfo getExecutionStepInfo() {
        return executionStepInfo;
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

    public ExecutionPath getExecutionPath() {
        return executionPath;
    }

    public ExecutionResultNode withNewChildren(List<ExecutionResultNode> children) {
        return transform(builder -> builder.children(children));
    }

    //
    public ExecutionResultNode withNewResolvedValue(ResolvedValue resolvedValue) {
        return transform(builder -> builder.resolvedValue(resolvedValue));
    }

    //
    public ExecutionResultNode withNewExecutionStepInfo(ExecutionStepInfo executionStepInfo) {
        return transform(builder -> builder.executionStepInfo(executionStepInfo));
    }

    public ExecutionResultNode withNewErrors(List<GraphQLError> errors) {
        return transform(builder -> builder.errors(errors));
    }

    public ExecutionResultNode withElapsedTime(ElapsedTime elapsedTime) {
        return transform(builder -> builder.elapsedTime(elapsedTime));
    }

    public abstract <B extends BuilderBase<B>> ExecutionResultNode transform(Consumer<B> builderConsumer);


    @Override
    public String toString() {
        return "ExecutionResultNode{" +
                "path=" + executionPath +
                ", executionStepInfo=" + executionStepInfo +
                ", resolvedValue=" + resolvedValue +
                ", nonNullableFieldWasNullException=" + nonNullableFieldWasNullException +
                ", children=" + children +
                ", errors=" + errors +
                '}';
    }

    public abstract static class BuilderBase<T extends BuilderBase<T>> {
        protected ExecutionStepInfo executionStepInfo;
        protected ResolvedValue resolvedValue;
        protected NonNullableFieldWasNullException nonNullableFieldWasNullException;
        protected List<ExecutionResultNode> children = new ArrayList<>();
        protected List<GraphQLError> errors = new ArrayList<>();
        protected ElapsedTime elapsedTime;
        protected ExecutionPath executionPath;

        public BuilderBase() {

        }

        public BuilderBase(ExecutionResultNode existing) {
            this.executionStepInfo = existing.getExecutionStepInfo();
            this.resolvedValue = existing.getResolvedValue();
            this.nonNullableFieldWasNullException = existing.getNonNullableFieldWasNullException();
            this.children.addAll(existing.getChildren());
            this.errors.addAll(existing.getErrors());
            this.elapsedTime = existing.getElapsedTime();
            this.executionPath = existing.getExecutionPath();
        }

        public abstract ExecutionResultNode build();

        public T resolvedValue(ResolvedValue resolvedValue) {
            this.resolvedValue = resolvedValue;
            return (T) this;
        }

        public T executionStepInfo(ExecutionStepInfo executionStepInfo) {
            this.executionStepInfo = executionStepInfo;
            return (T) this;
        }

        public T nonNullableFieldWasNullException(NonNullableFieldWasNullException nonNullableFieldWasNullException) {
            this.nonNullableFieldWasNullException = nonNullableFieldWasNullException;
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

    }
}
