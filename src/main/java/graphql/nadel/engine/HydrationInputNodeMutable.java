package graphql.nadel.engine;

//import graphql.execution.ExecutionStepInfo;
//import graphql.execution.NonNullableFieldWasNullException;
//import graphql.execution.nextgen.result.ResolvedValue;
//import graphql.nadel.engine.transformation.HydrationTransformation;
//import graphql.nadel.engine.transformation.HydrationTransformationMutable;
//import graphql.nadel.execution.LeafExecutionResultNode;
//
//public class HydrationInputNodeMutable extends LeafExecutionResultNode {
//
//    private final HydrationTransformationMutable hydrationTransformation;
//
//    public HydrationInputNodeMutable(HydrationTransformationMutable hydrationTransformation,
//                                     ExecutionStepInfo executionStepInfo,
//                                     ResolvedValue resolvedValue,
//                                     NonNullableFieldWasNullException nonNullableFieldWasNullException) {
//        super(executionStepInfo, resolvedValue, nonNullableFieldWasNullException);
//        this.hydrationTransformation = hydrationTransformation;
//    }
//
//    public HydrationTransformationMutable getHydrationTransformation() {
//        return hydrationTransformation;
//    }
//}
