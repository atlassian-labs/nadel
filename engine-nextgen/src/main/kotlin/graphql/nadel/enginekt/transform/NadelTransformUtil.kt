package graphql.nadel.enginekt.transform

import graphql.introspection.Introspection.TypeNameMetaFieldDef
import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.transform.artificial.AliasHelper
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.util.JsonMap
import graphql.normalized.NormalizedField
import graphql.normalized.NormalizedField.newNormalizedField
import graphql.schema.FieldCoordinates
import graphql.schema.FieldCoordinates.coordinates as makeFieldCoordinates

object NadelTransformUtil {
    fun getOverallTypeNameOfNode(
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        aliasHelper: AliasHelper,
        node: JsonNode,
    ): String? {
        @Suppress("UNCHECKED_CAST")
        val nodeValueAsMap = node.value as? JsonMap ?: return null

        return if (aliasHelper.typeNameResultKey in nodeValueAsMap) {
            executionBlueprint.getOverallTypeName(
                service = service,
                underlyingTypeName = nodeValueAsMap[aliasHelper.typeNameResultKey] as String,
            )
        } else {
            null
        }
    }

    fun makeTypeNameField(
        aliasHelper: AliasHelper,
        objectTypeNames: List<String>,
    ): NormalizedField {
        return newNormalizedField()
            .alias(aliasHelper.typeNameResultKey)
            .fieldName(TypeNameMetaFieldDef.name)
            .objectTypeNames(objectTypeNames)
            .build()
    }
}

fun <T : NadelFieldInstruction> Map<FieldCoordinates, T>.getInstructionForNode(
    executionBlueprint: NadelOverallExecutionBlueprint,
    service: Service,
    aliasHelper: AliasHelper,
    parentNode: JsonNode,
): T? = let { instructions ->
    val overallTypeName = NadelTransformUtil.getOverallTypeNameOfNode(
        executionBlueprint = executionBlueprint,
        service = service,
        aliasHelper = aliasHelper,
        node = parentNode,
    )

    // NOTE: the given instructions must have tho same field name, just differing type name
    // Otherwise this function doesn't make sense
    val fieldName = instructions.keys.first().fieldName

    instructions[makeFieldCoordinates(overallTypeName, fieldName)]
}

