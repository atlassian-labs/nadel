package graphql.nadel.engine.transform

import graphql.introspection.Introspection.TypeNameMetaFieldDef
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.blueprint.NadelFieldInstruction
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
        service: Service,
        aliasHelper: NadelAliasHelper,
        node: JsonNode,
    ): String? {
        @Suppress("UNCHECKED_CAST")
        val nodeValueAsMap = node.value as? JsonMap ?: return null

        return if (aliasHelper.typeNameResultKey in nodeValueAsMap) {
            service.blueprint.typeRenames.getOverallName(
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
        aliasHelper: NadelAliasHelper,
    ): GraphQLFieldDefinition? {
        val overallTypeName = getOverallTypeNameOfNode(
            service = service,
            aliasHelper = aliasHelper,
            node = parentNode,
        ) ?: return null

        val coordinates = makeFieldCoordinates(overallTypeName, overallField.name)
        return service.schema.getField(coordinates)
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
        transformerFunction: (Any) -> Any?,
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
    service: Service,
    aliasHelper: NadelAliasHelper,
    parentNode: JsonNode,
): T? = let { instructions ->
    val overallTypeName = NadelTransformUtil.getOverallTypeNameOfNode(
        service = service,
        aliasHelper = aliasHelper,
        node = parentNode,
    ) ?: return null

    instructions[overallTypeName]
}

fun <T : NadelFieldInstruction> Map<GraphQLObjectTypeName, List<T>>.getInstructionsForNode(
    service: Service,
    aliasHelper: NadelAliasHelper,
    parentNode: JsonNode,
): List<T> = let { instructions ->
    val overallTypeName = NadelTransformUtil.getOverallTypeNameOfNode(
        service = service,
        aliasHelper = aliasHelper,
        node = parentNode,
    ) ?: return emptyList()

    instructions[overallTypeName] ?: emptyList()
}
