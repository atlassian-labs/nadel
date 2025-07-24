package graphql.nadel.engine.plan

import graphql.nadel.Service
import graphql.nadel.engine.transform.NadelTransformServiceExecutionContext
import graphql.nadel.engine.transform.NadelTransformWithContext
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters
import graphql.normalized.ExecutableNormalizedField

internal typealias AnyNadelExecutionPlanStep = NadelExecutionPlan.Step<Any, NadelTransformServiceExecutionContext>

data class NadelExecutionPlan(
    // this is a map for overall Fields
    val transformationSteps: Map<ExecutableNormalizedField, List<AnyNadelExecutionPlanStep>>,
) {
    data class Step<T : Any, U : NadelTransformServiceExecutionContext>(
        val service: Service,
        val field: ExecutableNormalizedField,
        val transform: NadelTransformWithContext<T, U>,
        val queryTransformTimingStep: NadelInstrumentationTimingParameters.Step,
        val resultTransformTimingStep: NadelInstrumentationTimingParameters.Step,
        val state: T,
        val context: U?,
    )

    /**
     * Creates and returns a new [NadelExecutionPlan] that is a merging of `this` plan
     * and the [other] plan.
     */
    fun merge(other: NadelExecutionPlan): NadelExecutionPlan {
        val newSteps = transformationSteps.toMutableMap()
        other.transformationSteps.forEach { (field, steps) ->
            newSteps.compute(field) { _, oldSteps ->
                oldSteps?.let { it + steps } ?: steps
            }
        }

        return copy(transformationSteps = newSteps)
    }
}
