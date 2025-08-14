package graphql.nadel.engine.plan

import graphql.nadel.engine.transform.GenericNadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldContext
import graphql.nadel.engine.transform.NadelTransformOperationContext
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters
import graphql.normalized.ExecutableNormalizedField

internal data class NadelExecutionPlan(
    /**
     * This is [Map] stores overall fields
     */
    val transformFieldSteps: Map<ExecutableNormalizedField, List<TransformFieldStep>>,
    val transformContexts: Map<GenericNadelTransform, NadelTransformOperationContext>,
) {
    internal data class TransformFieldStep(
        val transform: GenericNadelTransform,
        val transformFieldContext: NadelTransformFieldContext<NadelTransformOperationContext>,
        val queryTransformTimingStep: NadelInstrumentationTimingParameters.Step,
        val resultTransformTimingStep: NadelInstrumentationTimingParameters.Step,
    )
}
