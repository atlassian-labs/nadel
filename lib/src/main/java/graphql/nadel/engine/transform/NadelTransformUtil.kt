package graphql.nadel.engine.transform

import graphql.introspection.Introspection.TypeNameMetaFieldDef
import graphql.nadel.Service
import graphql.nadel.ServiceLike
import graphql.nadel.engine.blueprint.NadelFieldInstruction
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.util.JsonMap
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.ExecutableNormalizedField.newNormalizedField
import graphql.normalized.incremental.NormalizedDeferredExecution

/**
 * Try to limit the usage of this to limit potential refactoring issues in the future.
 *
 * i.e. try to use preexisting functions, and never make this public
 */
private fun getOverallTypeNameOfNode(
    executionBlueprint: NadelOverallExecutionBlueprint,
    service: ServiceLike,
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
    deferredExecutions: LinkedHashSet<NormalizedDeferredExecution>,
): ExecutableNormalizedField {
    return newNormalizedField()
        .alias(aliasHelper.typeNameResultKey)
        .fieldName(TypeNameMetaFieldDef.name)
        .objectTypeNames(objectTypeNames)
        .deferredExecutions(deferredExecutions)
        .build()
}

/**
 * Gets the instruction for a node. There _must_ be a `__typename` selection in the [parentNode].
 */
fun <T : NadelFieldInstruction> Map<GraphQLObjectTypeName, T>.getInstructionForNode(
    executionBlueprint: NadelOverallExecutionBlueprint,
    service: ServiceLike,
    aliasHelper: NadelAliasHelper,
    parentNode: JsonNode,
): T? = let { instructions ->
    val overallTypeName = getOverallTypeNameOfNode(
        executionBlueprint = executionBlueprint,
        service = service,
        aliasHelper = aliasHelper,
        node = parentNode,
    ) ?: return null

    instructions[overallTypeName]
}

fun <T : NadelFieldInstruction> Map<GraphQLObjectTypeName, List<T>>.getInstructionsForNode(
    executionBlueprint: NadelOverallExecutionBlueprint,
    service: ServiceLike,
    aliasHelper: NadelAliasHelper,
    parentNode: JsonNode,
): List<T> = let { instructions ->
    val overallTypeName = getOverallTypeNameOfNode(
        executionBlueprint = executionBlueprint,
        service = service,
        aliasHelper = aliasHelper,
        node = parentNode,
    ) ?: return emptyList()

    instructions[overallTypeName] ?: emptyList()
}
