package graphql.nadel.enginekt.plan

import graphql.nadel.Service
import graphql.nadel.enginekt.transform.result.GraphQLResultTransform
import graphql.normalized.NormalizedField

data class GraphQLResultTransformationPlan(
    val resultTransformations: List<GraphQLResultTransformIntent>
)

data class GraphQLResultTransformIntent(
    val service: Service,
    val field: NormalizedField,
    val transform: GraphQLResultTransform,
)
