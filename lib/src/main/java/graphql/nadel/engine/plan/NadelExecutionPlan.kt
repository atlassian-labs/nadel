package graphql.nadel.engine.plan

import graphql.nadel.engine.transform.GenericNadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldContext
import graphql.nadel.engine.transform.NadelTransformOperationContext
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters
import graphql.normalized.ExecutableNormalizedField

data class NadelExecutionPlan(
    /**
     * This is [Map] stores overall fields
     */
    val transformationSteps: Map<ExecutableNormalizedField, List<Step>>,
    val transformContexts: Map<GenericNadelTransform, NadelTransformOperationContext>,
) {
    data class Step(
        val transform: GenericNadelTransform,
        val transformContext: NadelTransformFieldContext<NadelTransformOperationContext>,
        val queryTransformTimingStep: NadelInstrumentationTimingParameters.Step,
        val resultTransformTimingStep: NadelInstrumentationTimingParameters.Step,
    )
}
