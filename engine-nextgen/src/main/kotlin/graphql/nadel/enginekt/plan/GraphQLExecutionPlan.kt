package graphql.nadel.enginekt.plan

import graphql.nadel.Service
import graphql.nadel.enginekt.transform.result.GraphQLResultTransform
import graphql.normalized.NormalizedField

data class GraphQLExecutionPlan(
    val schemaTransforms: List<GraphQLSchemaTransformation>,
    val resultTransformations: List<GraphQLResultTransformation>
)

sealed class GraphQLSchemaTransformation

data class GraphQLUnderlyingFieldTransformation(
    val field: NormalizedField,
    val underlyingName: String,
    val underlyingObjectTypeName: String,
) : GraphQLSchemaTransformation()

data class GraphQLResultTransformation(
    val service: Service,
    val field: NormalizedField,
    val transform: GraphQLResultTransform,
)
