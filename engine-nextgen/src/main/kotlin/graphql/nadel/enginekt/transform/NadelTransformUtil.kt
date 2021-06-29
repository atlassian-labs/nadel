package graphql.nadel.enginekt.transform

import graphql.introspection.Introspection.TypeNameMetaFieldDef
import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.transform.artificial.AliasHelper
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.util.JsonMap
import graphql.nadel.enginekt.util.getField
import graphql.nadel.enginekt.util.makeFieldCoordinates
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.ExecutableNormalizedField.newNormalizedField
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLFieldDefinition

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
    ): ExecutableNormalizedField {
        return newNormalizedField()
            .alias(aliasHelper.typeNameResultKey)
            .fieldName(TypeNameMetaFieldDef.name)
            .objectTypeNames(objectTypeNames)
            .build()
    }

    /**
     * Gets the field definition for a specific node using the `__typename` from the result node.
     *
     * @param overallField the field to get the definition for
     * @param parentNode the underlying parent node that has the field selected, used to get the type name
     * @param service the service that the parent node was returned from
     */
    fun getOverallFieldDef(
        // Subject arguments
        overallField: ExecutableNormalizedField,
        parentNode: JsonNode,
        service: Service,
        // Supplementary arguments
        executionBlueprint: NadelOverallExecutionBlueprint,
        aliasHelper: AliasHelper,
    ): GraphQLFieldDefinition? {
        val overallTypeName = getOverallTypeNameOfNode(
            executionBlueprint = executionBlueprint,
            service = service,
            aliasHelper = aliasHelper,
            node = parentNode,
        ) ?: return null

        val coordinates = makeFieldCoordinates(overallTypeName, overallField.name)
        return executionBlueprint.schema.getField(coordinates)
    }
}

/**
 * Gets the instruction for a node. There _must_ be a `__typename` selection in the [parentNode].
 */
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
    ) ?: return null

    // NOTE: the given instructions must have tho same field name, just differing type name
    // Otherwise this function doesn't make sense
    val fieldName = instructions.keys.first().fieldName

    instructions[makeFieldCoordinates(overallTypeName, fieldName)]
}

