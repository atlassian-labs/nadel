package graphql.nadel.engine;

import graphql.execution.ExecutionStepInfo;
import graphql.execution.NonNullableFieldWasNullException;
import graphql.execution.nextgen.result.LeafExecutionResultNode;
import graphql.execution.nextgen.result.ResolvedValue;
import graphql.nadel.engine.transformation.HydrationTransformation;

public class HydrationInputNode extends LeafExecutionResultNode {

    private final HydrationTransformation hydrationTransformation;
    private final String fieldName;
    private boolean insideList;

    public HydrationInputNode(HydrationTransformation hydrationTransformation,
                              String fieldName,
                              boolean insideList,
                              ExecutionStepInfo executionStepInfo,
                              ResolvedValue resolvedValue,
                              NonNullableFieldWasNullException nonNullableFieldWasNullException) {
        super(executionStepInfo, resolvedValue, nonNullableFieldWasNullException);
        this.hydrationTransformation = hydrationTransformation;
        this.fieldName = fieldName;
        this.insideList = insideList;
    }

    public String getFieldName() {
        return fieldName;
    }

    public boolean isInsideList() {
        return insideList;
    }

    public HydrationTransformation getHydrationTransformation() {
        return hydrationTransformation;
    }
}
