package graphql.nadel.result;

import graphql.Assert;
import graphql.GraphQLError;
import graphql.Internal;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.execution.NonNullableFieldWasNullException;
import graphql.execution.nextgen.result.ResolvedValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static graphql.Assert.assertNotNull;

@Internal
public abstract class ExecutionResultNode {

    private final ExecutionStepInfo executionStepInfo;
    private final ResolvedValue resolvedValue;
    private final NonNullableFieldWasNullException nonNullableFieldWasNullException;
    private final List<ExecutionResultNode> children;
    private final List<GraphQLError> errors;
    private final ElapsedTime elapsedTime;
    private final AtomicInteger nodeCount = new AtomicInteger(0);

    /*
     * we are trusting here the the children list is not modified on the outside (no defensive copy)
     */
    protected ExecutionResultNode(ExecutionStepInfo executionStepInfo,
                                  ResolvedValue resolvedValue,
                                  NonNullableFieldWasNullException nonNullableFieldWasNullException,
                                  List<ExecutionResultNode> children,
                                  List<GraphQLError> errors,
                                  ElapsedTime elapsedTime) {
        this.resolvedValue = resolvedValue;
        this.executionStepInfo = executionStepInfo;
        this.nonNullableFieldWasNullException = nonNullableFieldWasNullException;
        this.children = Collections.unmodifiableList(assertNotNull(children));
        children.forEach(Assert::assertNotNull);
        this.errors = Collections.unmodifiableList(errors);
        this.elapsedTime = elapsedTime;
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

    public Integer getResultNodeCount() {
        return nodeCount.get();
    }

    public void setNodeCount(AtomicInteger newNodeCount) {
        nodeCount.set(newNodeCount.get());
    }

    /**
     * Creates a new ExecutionResultNode of the same specific type with the new set of result children
     *
     * @param children the new children for this result node
     *
     * @return a new ExecutionResultNode with the new result children
     */
    public abstract ExecutionResultNode withNewChildren(List<ExecutionResultNode> children);

    public abstract ExecutionResultNode withNewResolvedValue(ResolvedValue resolvedValue);

    public abstract ExecutionResultNode withNewExecutionStepInfo(ExecutionStepInfo executionStepInfo);


    /**
     * Creates a new ExecutionResultNode of the same specific type with the new error collection
     *
     * @param errors the new errors for this result node
     *
     * @return a new ExecutionResultNode with the new errors
     */
    public abstract ExecutionResultNode withNewErrors(List<GraphQLError> errors);

    public abstract ExecutionResultNode withElapsedTime(ElapsedTime elapsedTime);


    @Override
    public String toString() {
        return "ExecutionResultNode{" +
                "executionStepInfo=" + executionStepInfo +
                ", resolvedValue=" + resolvedValue +
                ", nonNullableFieldWasNullException=" + nonNullableFieldWasNullException +
                ", children=" + children +
                ", errors=" + errors +
                '}';
    }

    public abstract class Builder {
        protected ExecutionStepInfo executionStepInfo;
        protected ResolvedValue resolvedValue;
        protected NonNullableFieldWasNullException nonNullableFieldWasNullException;
        protected List<ExecutionResultNode> children;
        protected List<GraphQLError> errors;
        protected ElapsedTime elapsedTime;

    }
}
