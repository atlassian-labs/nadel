package graphql.nadel.execution;

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

@Internal
public abstract class ExecutionResultNode {

    private ExecutionStepInfo executionStepInfo;
    private ResolvedValue resolvedValue;
    private NonNullableFieldWasNullException nonNullableFieldWasNullException;
    private List<ExecutionResultNode> children;
    private List<GraphQLError> errors;

    /*
     * we are trusting here the the children list is not modified on the outside (no defensive copy)
     */
    protected ExecutionResultNode(ExecutionStepInfo executionStepInfo,
                                  ResolvedValue resolvedValue,
                                  NonNullableFieldWasNullException nonNullableFieldWasNullException,
                                  List<ExecutionResultNode> children,
                                  List<GraphQLError> errors) {
        this.resolvedValue = resolvedValue;
        this.executionStepInfo = executionStepInfo;
        this.nonNullableFieldWasNullException = nonNullableFieldWasNullException;
        this.children = children;
        children.forEach(Assert::assertNotNull);
        this.errors = Collections.unmodifiableList(errors);
    }

    public List<GraphQLError> getErrors() {
        return new ArrayList<>(errors);
    }

    public void setExecutionStepInfo(ExecutionStepInfo executionStepInfo) {
        this.executionStepInfo = executionStepInfo;
    }

    public void setResolvedValue(ResolvedValue resolvedValue) {
        this.resolvedValue = resolvedValue;
    }

    public void setNonNullableFieldWasNullException(NonNullableFieldWasNullException nonNullableFieldWasNullException) {
        this.nonNullableFieldWasNullException = nonNullableFieldWasNullException;
    }

    public void setErrors(List<GraphQLError> errors) {
        this.errors = errors;
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


    public void addChild(ExecutionResultNode executionResultNode) {
        this.children.add(executionResultNode);
    }

    public void replaceChild(int index, ExecutionResultNode executionResultNode) {
        this.children.set(index, executionResultNode);
    }

    public void setChildren(List<ExecutionResultNode> children) {
        this.children = children;
    }

    public Optional<NonNullableFieldWasNullException> getChildNonNullableException() {
        return children.stream()
                .filter(executionResultNode -> executionResultNode.getNonNullableFieldWasNullException() != null)
                .map(ExecutionResultNode::getNonNullableFieldWasNullException)
                .findFirst();
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


//    @Override
//    public String toString() {
//        return getClass().getSimpleName() + "{" +
//                "executionStepInfo=" + executionStepInfo +
//                ", resolvedValue=" + resolvedValue +
//                ", nonNullableFieldWasNullException=" + nonNullableFieldWasNullException +
//                ", children=" + children +
//                ", errors=" + errors +
//                '}';
//    }
}
