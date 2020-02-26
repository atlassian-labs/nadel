package graphql.nadel.result;

import graphql.GraphQLError;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.nextgen.result.ResolvedValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UnresolvedObjectResultNode extends ObjectExecutionResultNode {

    public UnresolvedObjectResultNode(ExecutionStepInfo executionStepInfo, ResolvedValue resolvedValue) {
        super(executionStepInfo, resolvedValue, Collections.emptyList(), Collections.emptyList());
    }

    public UnresolvedObjectResultNode(ExecutionStepInfo executionStepInfo, ResolvedValue resolvedValue, List<ExecutionResultNode> children, List<GraphQLError> errors, ElapsedTime elapsedTime) {
        super(executionStepInfo, resolvedValue, children, errors, elapsedTime);
    }

    @Override
    public UnresolvedObjectResultNode withNewChildren(List<ExecutionResultNode> children) {
        return new UnresolvedObjectResultNode(getExecutionStepInfo(), getResolvedValue(), children, getErrors(), getElapsedTime());
    }

    @Override
    public UnresolvedObjectResultNode withNewResolvedValue(ResolvedValue resolvedValue) {
        return new UnresolvedObjectResultNode(getExecutionStepInfo(), resolvedValue, getChildren(), getErrors(), getElapsedTime());
    }

    @Override
    public UnresolvedObjectResultNode withNewExecutionStepInfo(ExecutionStepInfo executionStepInfo) {
        return new UnresolvedObjectResultNode(executionStepInfo, getResolvedValue(), getChildren(), getErrors(), getElapsedTime());
    }

    @Override
    public UnresolvedObjectResultNode withNewErrors(List<GraphQLError> errors) {
        return new UnresolvedObjectResultNode(getExecutionStepInfo(), getResolvedValue(), getChildren(), new ArrayList<>(errors), getElapsedTime());
    }

    @Override
    public UnresolvedObjectResultNode withElapsedTime(ElapsedTime elapsedTime) {
        return new UnresolvedObjectResultNode(getExecutionStepInfo(), getResolvedValue(), getChildren(), getErrors(), elapsedTime);
    }
}