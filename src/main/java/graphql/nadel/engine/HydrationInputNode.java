package graphql.nadel.engine;

import graphql.GraphQLError;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.NonNullableFieldWasNullException;
import graphql.execution.nextgen.result.ResolvedValue;
import graphql.nadel.engine.transformation.HydrationTransformation;
import graphql.nadel.result.ElapsedTime;
import graphql.nadel.result.LeafExecutionResultNode;

import java.util.List;

public class HydrationInputNode extends LeafExecutionResultNode {

    private final HydrationTransformation hydrationTransformation;

    public HydrationInputNode(HydrationTransformation hydrationTransformation,
                              ExecutionStepInfo executionStepInfo,
                              ResolvedValue resolvedValue,
                              NonNullableFieldWasNullException nonNullableFieldWasNullException,
                              List<GraphQLError> errors,
                              ElapsedTime elapsedTime) {
        super(executionStepInfo, resolvedValue, nonNullableFieldWasNullException, errors, elapsedTime);
        this.hydrationTransformation = hydrationTransformation;
    }

    public HydrationTransformation getHydrationTransformation() {
        return hydrationTransformation;
    }
}
