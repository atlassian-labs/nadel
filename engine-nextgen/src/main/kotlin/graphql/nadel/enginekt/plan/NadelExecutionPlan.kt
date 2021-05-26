package graphql.nadel.enginekt.plan

import graphql.nadel.Service
import graphql.nadel.enginekt.transform.query.NadelTransform
import graphql.normalized.NormalizedField

internal typealias AnyNadelExecutionPlanStep = NadelExecutionPlan.Step<Any>

internal data class NadelExecutionPlan(
    val transformationSteps: Map<NormalizedField, List<AnyNadelExecutionPlanStep>>,
) {
    data class Step<T : Any>(
        val service: Service,
        val field: NormalizedField,
        val transform: NadelTransform<T>,
        val state: T,
    )
}

