package graphql.nadel.enginekt.plan

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelRenameFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelTypeRenameInstruction
import graphql.nadel.enginekt.transform.result.NadelResultTransform
import graphql.normalized.NormalizedField

internal data class NadelExecutionPlan(
    val schemaTransforms: Map<NormalizedField, List<NadelSchemaTransformation>>,
    val resultTransformations: Map<NormalizedField, List<NadelResultTransformation>>,
)

internal sealed class NadelSchemaTransformation {
    abstract val field: NormalizedField
}

internal data class NadelUnderlyingTypeTransformation(
    override val field: NormalizedField,
    val typeRenameInstruction: NadelTypeRenameInstruction,
) : NadelSchemaTransformation() {
    init {
        // Field must be in terms of overall schema so predicate must return true
        require(field.objectType.name == typeRenameInstruction.overallName)
    }
}

internal data class NadelUnderlyingFieldTransformation(
    override val field: NormalizedField,
    val fieldRenameInstruction: NadelRenameFieldInstruction,
) : NadelSchemaTransformation() {
    init {
        // Field must be in terms of overall schema so predicate must return true
        require(field.name == fieldRenameInstruction.location.fieldName)
    }
}

data class NadelResultTransformation(
    val service: Service,
    val field: NormalizedField,
    val transform: NadelResultTransform,
)
