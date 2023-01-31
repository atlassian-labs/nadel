package graphql.nadel.engine.transform

import graphql.introspection.Introspection.TypeNameMetaFieldDef
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.blueprint.NadelFieldInstruction
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.getField
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.engine.util.queryPath
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.ExecutableNormalizedField.newNormalizedField
import graphql.schema.GraphQLFieldDefinition

object NadelTransformUtil {
    fun getOverallTypeNameOfNode(
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        aliasHelper: NadelAliasHelper,
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
        aliasHelper: NadelAliasHelper,
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
        aliasHelper: NadelAliasHelper,
    ): GraphQLFieldDefinition? {
        val overallTypeName = getOverallTypeNameOfNode(
            executionBlueprint = executionBlueprint,
            service = service,
            aliasHelper = aliasHelper,
            node = parentNode,
        ) ?: return null

        val coordinates = makeFieldCoordinates(overallTypeName, overallField.name)
        return executionBlueprint.engineSchema.getField(coordinates)
    }
}

/**
 * Gets the instruction for a node. There _must_ be a `__typename` selection in the [parentNode].
 */
fun <T : NadelFieldInstruction> Map<GraphQLObjectTypeName, T>.getInstructionForNode(
    executionBlueprint: NadelOverallExecutionBlueprint,
    service: Service,
    aliasHelper: NadelAliasHelper,
    parentNode: JsonNode,
): T? = let { instructions ->
    val overallTypeName = NadelTransformUtil.getOverallTypeNameOfNode(
        executionBlueprint = executionBlueprint,
        service = service,
        aliasHelper = aliasHelper,
        node = parentNode,
    ) ?: return null

    instructions[overallTypeName]
}

fun <T : NadelFieldInstruction> Map<GraphQLObjectTypeName, List<T>>.getInstructionsForNode(
    executionBlueprint: NadelOverallExecutionBlueprint,
    service: Service,
    aliasHelper: NadelAliasHelper,
    parentNode: JsonNode,
): List<T> = let { instructions ->
    val overallTypeName = NadelTransformUtil.getOverallTypeNameOfNode(
        executionBlueprint = executionBlueprint,
        service = service,
        aliasHelper = aliasHelper,
        node = parentNode,
    ) ?: return emptyList()

    instructions[overallTypeName] ?: emptyList()
}
