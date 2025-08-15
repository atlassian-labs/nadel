package graphql.nadel.engine.plan

import graphql.nadel.engine.NadelOperationExecutionContext
import graphql.nadel.engine.transform.GenericNadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldContext
import graphql.nadel.engine.transform.NadelTransformOperationContext
import graphql.nadel.engine.transform.NadelTransformTimingSteps
import graphql.normalized.ExecutableNormalizedField

internal data class NadelExecutionPlan(
    val operationExecutionContext: NadelOperationExecutionContext,
    /**
     * This is [Map] stores overall fields
     */
    val transformFieldSteps: Map<ExecutableNormalizedField, List<TransformFieldStep>>,
    val transformOperationContexts: Map<GenericNadelTransform, NadelTransformOperationContext>,
) {
    internal data class TransformFieldStep(
        val transform: GenericNadelTransform,
        val transformFieldContext: NadelTransformFieldContext<NadelTransformOperationContext>,
        val timingSteps: NadelTransformTimingSteps,
    )
}
