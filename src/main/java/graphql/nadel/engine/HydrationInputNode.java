package graphql.nadel.engine;

import graphql.execution.NonNullableFieldWasNullException;
import graphql.execution.nextgen.FetchedValueAnalysis;
import graphql.execution.nextgen.result.LeafExecutionResultNode;

public class HydrationInputNode extends LeafExecutionResultNode {

    public HydrationInputNode(FetchedValueAnalysis fetchedValueAnalysis, NonNullableFieldWasNullException nonNullableFieldWasNullException) {
        super(fetchedValueAnalysis, nonNullableFieldWasNullException);
    }
}
