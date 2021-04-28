package graphql.nadel.enginekt.plan

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.GraphQLRenameInstruction
import graphql.nadel.enginekt.blueprint.GraphQLUnderlyingType
import graphql.nadel.enginekt.transform.result.GraphQLResultTransform
import graphql.normalized.NormalizedField

data class GraphQLExecutionPlan(
    val schemaTransforms: Map<NormalizedField, List<GraphQLSchemaTransformation>>,
    val resultTransformations: Map<NormalizedField, List<GraphQLResultTransformation>>,
)

sealed class GraphQLSchemaTransformation {
    abstract val field: NormalizedField
}

data class GraphQLUnderlyingTypeTransformation(
    override val field: NormalizedField,
    val underlyingType: GraphQLUnderlyingType,
) : GraphQLSchemaTransformation() {
    init {
        // Field must be in terms of overall schema so predicate must return true
        require(field.objectType.name == underlyingType.overallName)
    }
}

data class GraphQLUnderlyingFieldTransformation(
        override val field: NormalizedField,
        val renameInstruction: GraphQLRenameInstruction,
) : GraphQLSchemaTransformation() {
    init {
        // Field must be in terms of overall schema so predicate must return true
        require(field.name == renameInstruction.overallName)
    }
}

data class GraphQLResultTransformation(
    val service: Service,
    val field: NormalizedField,
    val transform: GraphQLResultTransform,
)
