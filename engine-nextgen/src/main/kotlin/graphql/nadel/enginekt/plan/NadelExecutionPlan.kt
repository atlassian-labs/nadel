package graphql.nadel.enginekt.plan

import graphql.nadel.Service
import graphql.nadel.enginekt.transform.query.NadelTransform
import graphql.normalized.NormalizedField

internal data class NadelExecutionPlan(
    val transformations: Map<NormalizedField, List<Step>>,
) {
    data class Step(
        val service: Service,
        val field: NormalizedField,
        val transform: NadelTransform<Any>,
        val state: Any,
    )
}

