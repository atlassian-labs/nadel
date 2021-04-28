package graphql.nadel.enginekt.plan

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelRenameInstruction
import graphql.nadel.enginekt.blueprint.NadelUnderlyingType
import graphql.nadel.enginekt.transform.result.GraphQLResultTransform
import graphql.normalized.NormalizedField

data class NadelExecutionPlan(
    val schemaTransforms: Map<NormalizedField, List<NadelSchemaTransformation>>,
    val resultTransformations: Map<NormalizedField, List<GraphQLResultTransformation>>,
)

sealed class NadelSchemaTransformation {
    abstract val field: NormalizedField
}

data class NadelUnderlyingTypeTransformation(
    override val field: NormalizedField,
    val underlyingType: NadelUnderlyingType,
) : NadelSchemaTransformation() {
    init {
        // Field must be in terms of overall schema so predicate must return true
        require(field.objectType.name == underlyingType.overallName)
    }
}

data class NadelUnderlyingFieldTransformation(
    override val field: NormalizedField,
    val renameInstruction: NadelRenameInstruction,
) : NadelSchemaTransformation() {
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
