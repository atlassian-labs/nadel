package graphql.nadel.enginekt.plan

import graphql.nadel.Service
import graphql.nadel.enginekt.transform.NadelTransform
import graphql.normalized.NormalizedField

internal typealias AnyNadelExecutionPlanStep = NadelExecutionPlan.Step<Any>

data class NadelExecutionPlan(
    // this is a map for overall Fields
    val transformationSteps: Map<NormalizedField, List<AnyNadelExecutionPlanStep>>,
) {
    data class Step<T : Any>(
        val service: Service,
        val field: NormalizedField,
        val transform: NadelTransform<T>,
        val state: T,
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
