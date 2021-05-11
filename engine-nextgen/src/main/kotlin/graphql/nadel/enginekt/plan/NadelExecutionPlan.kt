package graphql.nadel.enginekt.plan

import graphql.nadel.Service
import graphql.nadel.enginekt.transform.result.NadelResultTransform
import graphql.normalized.NormalizedField

internal data class NadelExecutionPlan(
    // TODO: add query transforms here - I tried and got destroyed by generics
    val resultTransformations: Map<NormalizedField, List<NadelResultTransformation>>,
)

data class NadelResultTransformation(
    val service: Service,
    val field: NormalizedField,
    val transform: NadelResultTransform,
)
