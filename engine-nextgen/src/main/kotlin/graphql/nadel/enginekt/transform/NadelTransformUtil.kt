package graphql.nadel.enginekt.transform

import graphql.introspection.Introspection.TypeNameMetaFieldDef
import graphql.nadel.enginekt.blueprint.NadelFieldInstruction
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.transform.artificial.ArtificialFields
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.util.JsonMap
import graphql.normalized.NormalizedField
import graphql.normalized.NormalizedField.newNormalizedField
import graphql.schema.FieldCoordinates
import graphql.schema.FieldCoordinates.coordinates as makeFieldCoordinates

object NadelTransformUtil {
    fun getOverallTypename(
        executionPlan: NadelExecutionPlan,
        artificialFields: ArtificialFields,
        node: JsonNode,
    ): String? {
        @Suppress("UNCHECKED_CAST")
        val nodeValueAsMap = node.value as? JsonMap ?: return null

        return if (artificialFields.typeNameResultKey in nodeValueAsMap) {
            executionPlan.getOverallTypeName(
                underlyingTypeName = nodeValueAsMap[artificialFields.typeNameResultKey] as String,
            )
        } else {
            null
        }
    }

    fun makeTypeNameField(
        artificialFields: ArtificialFields,
        objectTypeNames: List<String>,
    ): NormalizedField {
        return newNormalizedField()
            .alias(artificialFields.typeNameResultKey)
            .fieldName(TypeNameMetaFieldDef.name)
            .objectTypeNames(objectTypeNames)
            .build()
    }
}

fun <T : NadelFieldInstruction> Map<FieldCoordinates, T>.getInstructionForNode(
    executionPlan: NadelExecutionPlan,
    artificialFields: ArtificialFields,
    parentNode: JsonNode,
): T? = let { instructions ->
    val overallTypeName = NadelTransformUtil.getOverallTypename(
        executionPlan = executionPlan,
        artificialFields = artificialFields,
        node = parentNode,
    )

    // NOTE: the given instructions must have tho same field name, just differing type name
    // Otherwise this function doesn't make sense
    val fieldName = instructions.keys.first().fieldName

    instructions[makeFieldCoordinates(overallTypeName, fieldName)]
}

