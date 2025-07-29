package graphql.nadel.engine.plan

import graphql.nadel.Service
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformServiceExecutionContext
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters
import graphql.normalized.ExecutableNormalizedField

internal typealias AnyNadelExecutionPlanStep = NadelExecutionPlan.Step<Any>

data class NadelExecutionPlan(
    // this is a map for overall Fields
    val transformationSteps: Map<ExecutableNormalizedField, List<AnyNadelExecutionPlanStep>>,
    val transformContexts: Map<NadelTransform<Any>, NadelTransformServiceExecutionContext?>,
) {
    data class Step<T : Any>(
        val service: Service,
        val field: ExecutableNormalizedField,
        val transform: NadelTransform<T>,
        val queryTransformTimingStep: NadelInstrumentationTimingParameters.Step,
        val resultTransformTimingStep: NadelInstrumentationTimingParameters.Step,
        val state: T,
    )
}
