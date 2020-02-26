package graphql.nadel.result;

import graphql.Assert;
import graphql.GraphQLError;
import graphql.Internal;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.NonNullableFieldWasNullException;
import graphql.execution.nextgen.result.ResolvedValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Internal
public class LeafExecutionResultNode extends ExecutionResultNode {

    public LeafExecutionResultNode(ExecutionStepInfo executionStepInfo,
                                   ResolvedValue resolvedValue,
                                   NonNullableFieldWasNullException nonNullableFieldWasNullException) {
        this(executionStepInfo, resolvedValue, nonNullableFieldWasNullException, Collections.emptyList());
    }

    public LeafExecutionResultNode(ExecutionStepInfo executionStepInfo,
                                   ResolvedValue resolvedValue,
                                   NonNullableFieldWasNullException nonNullableFieldWasNullException,
                                   List<GraphQLError> errors) {
        super(executionStepInfo, resolvedValue, nonNullableFieldWasNullException, Collections.emptyList(), errors, null);
    }

    public LeafExecutionResultNode(ExecutionStepInfo executionStepInfo,
                                   ResolvedValue resolvedValue,
                                   NonNullableFieldWasNullException nonNullableFieldWasNullException,
                                   List<GraphQLError> errors,
                                   ElapsedTime elapsedTime) {
        super(executionStepInfo, resolvedValue, nonNullableFieldWasNullException, Collections.emptyList(), errors, elapsedTime);
    }


    public Object getValue() {
        return getResolvedValue().getCompletedValue();
    }

    @Override
    public ExecutionResultNode withNewChildren(List<ExecutionResultNode> children) {
        return Assert.assertShouldNeverHappen();
    }

    @Override
    public ExecutionResultNode withNewExecutionStepInfo(ExecutionStepInfo executionStepInfo) {
        return new LeafExecutionResultNode(executionStepInfo, getResolvedValue(), getNonNullableFieldWasNullException(), getErrors(), getElapsedTime());
    }

    @Override
    public ExecutionResultNode withNewResolvedValue(ResolvedValue resolvedValue) {
        return new LeafExecutionResultNode(getExecutionStepInfo(), resolvedValue, getNonNullableFieldWasNullException(), getErrors(), getElapsedTime());
    }

    @Override
    public ExecutionResultNode withNewErrors(List<GraphQLError> errors) {
        return new LeafExecutionResultNode(getExecutionStepInfo(), getResolvedValue(), getNonNullableFieldWasNullException(), new ArrayList<>(errors), getElapsedTime());
    }

    @Override
    public ExecutionResultNode withElapsedTime(ElapsedTime elapsedTime) {
        return new LeafExecutionResultNode(getExecutionStepInfo(), getResolvedValue(), getNonNullableFieldWasNullException(), getErrors(), elapsedTime);
    }
}