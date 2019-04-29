package graphql.nadel.engine;

import graphql.execution.NonNullableFieldWasNullException;
import graphql.execution.nextgen.FetchedValueAnalysis;
import graphql.execution.nextgen.result.LeafExecutionResultNode;
import graphql.nadel.engine.transformation.HydrationTransformation;

public class HydrationInputNode extends LeafExecutionResultNode {

    private final HydrationTransformation hydrationTransformation;

    public HydrationInputNode(HydrationTransformation hydrationTransformation, FetchedValueAnalysis originalValueAnalysis, NonNullableFieldWasNullException nonNullableFieldWasNullException) {
        super(originalValueAnalysis, nonNullableFieldWasNullException);
        this.hydrationTransformation = hydrationTransformation;
    }

    public HydrationTransformation getHydrationTransformation() {
        return hydrationTransformation;
    }
}
