package graphql.nadel.enginekt.transform

import graphql.introspection.Introspection.TypeNameMetaFieldDef
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.blueprint.NadelFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.transform.artificial.NadelAliasHelper
import graphql.nadel.enginekt.transform.query.NadelQueryPath
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.transform.result.json.JsonNodes
import graphql.nadel.enginekt.util.JsonMap
import graphql.nadel.enginekt.util.getField
import graphql.nadel.enginekt.util.makeFieldCoordinates
import graphql.nadel.enginekt.util.queryPath
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
        return executionBlueprint.schema.getField(coordinates)
    }

    /**
     * Creates a list of Set instructions that will be used to modified the values of the result data
     * for a particular field.
     *
     * This function will call the transformerFunction for all result nodes associated with the overallField. So the
     * caller doesn't need to worry about whether the overallField is a collection or an individual field.
     *
     * Note that the transformer will only be called for non-null values.
     */
    fun createSetInstructions(
        nodes: JsonNodes,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        overallField: ExecutableNormalizedField,
        transformerFunction: (Any) -> Any,
    ): List<NadelResultInstruction> {
        val parentQueryPath = underlyingParentField?.queryPath ?: NadelQueryPath.root

        val valueNodes: List<JsonNode> = nodes.getNodesAt(
            queryPath = parentQueryPath + overallField.resultKey,
            flatten = true
        )

        return valueNodes
            .mapNotNull { valueNode ->
                nodes.getNodeAt(valueNode.resultPath)?.let { jsonNode ->
                    NadelResultInstruction.Set(
                        valueNode.resultPath,
                        jsonNode.value?.let { value -> transformerFunction(value) },
                    )
                }
            }
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
