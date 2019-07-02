package graphql.nadel.engine;

import graphql.execution.ExecutionStepInfo;
import graphql.execution.NonNullableFieldWasNullException;
import graphql.execution.nextgen.FetchedValueAnalysis;
import graphql.execution.nextgen.result.LeafExecutionResultNode;
import graphql.execution.nextgen.result.ResolvedValue;
import graphql.nadel.engine.transformation.HydrationTransformation;

public class HydrationInputNode extends LeafExecutionResultNode {

    private final HydrationTransformation hydrationTransformation;

    public HydrationInputNode(HydrationTransformation hydrationTransformation,
                              ExecutionStepInfo executionStepInfo,
                              ResolvedValue resolvedValue,
                              NonNullableFieldWasNullException nonNullableFieldWasNullException) {
        super(executionStepInfo, resolvedValue, nonNullableFieldWasNullException);
        this.hydrationTransformation = hydrationTransformation;
    }

    public HydrationTransformation getHydrationTransformation() {
        return hydrationTransformation;
    }
}
