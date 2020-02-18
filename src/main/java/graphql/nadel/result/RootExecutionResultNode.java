package graphql.nadel.result;

import graphql.GraphQLError;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.nextgen.result.ResolvedValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static graphql.Assert.assertShouldNeverHappen;

public class RootExecutionResultNode extends ObjectExecutionResultNode {


    public RootExecutionResultNode(List<ExecutionResultNode> children, List<GraphQLError> errors, ElapsedTime elapsedTime) {
        super(null, null, children, errors, elapsedTime);
    }

    public RootExecutionResultNode(List<ExecutionResultNode> children) {
        super(null, null, children, Collections.emptyList());
    }

    @Override
    public ExecutionStepInfo getExecutionStepInfo() {
        return assertShouldNeverHappen("not supported at root node");
    }

    @Override
    public ResolvedValue getResolvedValue() {
        return assertShouldNeverHappen("not supported at root node");
    }

    @Override
    public RootExecutionResultNode withNewChildren(List<ExecutionResultNode> children) {
        return new RootExecutionResultNode(children, getErrors(), getElapsedTime());
    }

    @Override
    public ExecutionResultNode withNewResolvedValue(ResolvedValue resolvedValue) {
        return assertShouldNeverHappen("not supported at root node");
    }

    @Override
    public ExecutionResultNode withNewExecutionStepInfo(ExecutionStepInfo executionStepInfo) {
        return assertShouldNeverHappen("not supported at root node");
    }

    @Override
    public RootExecutionResultNode withNewErrors(List<GraphQLError> errors) {
        return new RootExecutionResultNode(getChildren(), new ArrayList<>(errors), getElapsedTime());
    }

    @Override
    public RootExecutionResultNode withElapsedTime(ElapsedTime elapsedTime) {
        return new RootExecutionResultNode(getChildren(), getErrors(), elapsedTime);
    }
}
