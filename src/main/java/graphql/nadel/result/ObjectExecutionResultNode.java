package graphql.nadel.result;

import graphql.GraphQLError;
import graphql.Internal;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.nextgen.result.ResolvedValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Internal
public class ObjectExecutionResultNode extends ExecutionResultNode {


    public ObjectExecutionResultNode(ExecutionStepInfo executionStepInfo,
                                     ResolvedValue resolvedValue,
                                     List<ExecutionResultNode> children) {
        this(executionStepInfo, resolvedValue, children, Collections.emptyList());

    }

    public ObjectExecutionResultNode(ExecutionStepInfo executionStepInfo,
                                     ResolvedValue resolvedValue,
                                     List<ExecutionResultNode> children,
                                     ElapsedTime elapsedTime) {
        this(executionStepInfo, resolvedValue, children, Collections.emptyList(), elapsedTime);

    }

    public ObjectExecutionResultNode(ExecutionStepInfo executionStepInfo,
                                     ResolvedValue resolvedValue,
                                     List<ExecutionResultNode> children,
                                     List<GraphQLError> errors) {
        super(executionStepInfo, resolvedValue, ResultNodesUtil.newNullableException(executionStepInfo, children), children, errors, null, 0);
    }

    public ObjectExecutionResultNode(ExecutionStepInfo executionStepInfo,
                                     ResolvedValue resolvedValue,
                                     List<ExecutionResultNode> children,
                                     List<GraphQLError> errors,
                                     ElapsedTime elapsedTime) {
        super(executionStepInfo, resolvedValue, ResultNodesUtil.newNullableException(executionStepInfo, children), children, errors, elapsedTime, 0);
    }

    public ObjectExecutionResultNode(ExecutionStepInfo executionStepInfo,
                                     ResolvedValue resolvedValue,
                                     List<ExecutionResultNode> children,
                                     List<GraphQLError> errors,
                                     ElapsedTime elapsedTime,
                                     int totalNodeCount) {
        super(executionStepInfo, resolvedValue, ResultNodesUtil.newNullableException(executionStepInfo, children), children, errors, elapsedTime, totalNodeCount);
    }


    @Override
    public ObjectExecutionResultNode withNewChildren(List<ExecutionResultNode> children) {
        return new ObjectExecutionResultNode(getExecutionStepInfo(), getResolvedValue(), children, getErrors(), getElapsedTime(), getTotalNodeCount());
    }

    @Override
    public ExecutionResultNode withNewResolvedValue(ResolvedValue resolvedValue) {
        return new ObjectExecutionResultNode(getExecutionStepInfo(), resolvedValue, getChildren(), getErrors(), getElapsedTime(), getTotalNodeCount());
    }

    @Override
    public ExecutionResultNode withNewExecutionStepInfo(ExecutionStepInfo executionStepInfo) {
        return new ObjectExecutionResultNode(executionStepInfo, getResolvedValue(), getChildren(), getErrors(), getElapsedTime(), getTotalNodeCount());
    }

    @Override
    public ExecutionResultNode withNewErrors(List<GraphQLError> errors) {
        return new ObjectExecutionResultNode(getExecutionStepInfo(), getResolvedValue(), getChildren(), new ArrayList<>(errors), getElapsedTime(), getTotalNodeCount());
    }

    @Override
    public ExecutionResultNode withElapsedTime(ElapsedTime elapsedTime) {
        return new ObjectExecutionResultNode(getExecutionStepInfo(), getResolvedValue(), getChildren(), getErrors(), elapsedTime, getTotalNodeCount());
    }

    @Override
    public ExecutionResultNode withNodeCount(int nodeCount) {
        return new ObjectExecutionResultNode(getExecutionStepInfo(), getResolvedValue(), getChildren(), getErrors(), getElapsedTime(), nodeCount);
    }
}
