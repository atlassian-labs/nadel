package graphql.nadel.enginekt.transform.hydration.batch

import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationActorInputDef
import graphql.nadel.enginekt.transform.NadelTransformUtil
import graphql.nadel.enginekt.transform.hydration.NadelHydrationUtil.getHydrationActorNodes
import graphql.nadel.enginekt.transform.query.NadelQueryPath
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.asMutable
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.util.AnyList
import graphql.nadel.enginekt.util.AnyMap
import graphql.nadel.enginekt.util.JsonMap
import graphql.nadel.enginekt.util.asNullableJsonMap
import graphql.nadel.enginekt.util.emptyOrSingle
import graphql.nadel.enginekt.util.flatten
import graphql.nadel.enginekt.util.isList
import graphql.nadel.enginekt.util.queryPath
import graphql.nadel.enginekt.util.unwrapNonNull

internal object NadelBatchHydrationByObjectId {
    fun getHydrateInstructionsMatchingObjectId(
        executionBlueprint: NadelOverallExecutionBlueprint,
        state: NadelBatchHydrationTransform.State,
        instruction: NadelBatchHydrationFieldInstruction,
        parentNodes: List<JsonNode>,
        batches: List<ServiceExecutionResult>,
        matchStrategy: NadelBatchHydrationMatchStrategy.MatchObjectIdentifier,
    ): List<NadelResultInstruction> {
        // Associate by does not need to be strict here
        val resultNodesByObjectId = getHydrationActorNodes(instruction, batches)
            .asSequence()
            .map(JsonNode::value)
            .flatten(recursively = true)
            .mapNotNull {
                when (it) {
                    is AnyMap? -> it.asNullableJsonMap()
                    else -> error("Hydration actor result must be an object")
                }
            }
            .associateBy {
                // We don't want to show this in the overall result, so remove it here as we use it
                it.asMutable().remove(state.aliasHelper.getResultKey(matchStrategy.objectId))
            }

        val resultKeysToObjectIdOnHydrationParentNode = state.aliasHelper.getQueryPath(
            getPathToObjectIdentifierOnHydrationParentNode(instruction),
        )

        return parentNodes.map { parentNode ->
            val parentNodeIdentifierNodes = JsonNodeExtractor.getNodesAt(
                rootNode = parentNode,
                queryPath = resultKeysToObjectIdOnHydrationParentNode,
            )

            getHydrateInstructionsForNodeMatchingObjectId(
                executionBlueprint = executionBlueprint,
                state = state,
                parentNode = parentNode,
                parentNodeIdentifierNodes = parentNodeIdentifierNodes,
                resultNodesByObjectId = resultNodesByObjectId
            )
        }
    }

    private fun getHydrateInstructionsForNodeMatchingObjectId(
        executionBlueprint: NadelOverallExecutionBlueprint,
        state: NadelBatchHydrationTransform.State,
        parentNode: JsonNode,
        parentNodeIdentifierNodes: List<JsonNode>,
        resultNodesByObjectId: Map<Any?, JsonMap>,
    ): NadelResultInstruction {
        val hydratedFieldDef = NadelTransformUtil.getOverallFieldDef(
            overallField = state.hydratedField,
            parentNode = parentNode,
            service = state.hydratedFieldService,
            executionBlueprint = executionBlueprint,
            aliasHelper = state.aliasHelper,
        ) ?: error("Unable to find field definition for ${state.hydratedField.queryPath}")

        val newValue: Any? = if (hydratedFieldDef.type.unwrapNonNull().isList) {
            // Set to null if there were no identifier nodes
            if (parentNodeIdentifierNodes.all { it.value == null }) {
                null
            } else {
                parentNodeIdentifierNodes
                    .flatMap { parentNodeIdentifierNode ->
                        when (val id = parentNodeIdentifierNode.value) {
                            null -> emptySequence()
                            is AnyList -> id.asSequence().flatten(recursively = true)
                            else -> sequenceOf(id)
                        }
                    }
                    .map { id ->
                        resultNodesByObjectId[id]
                    }
                    .toList()
            }
        } else {
            parentNodeIdentifierNodes.emptyOrSingle()?.let { node ->
                resultNodesByObjectId[node.value]
            }
        }

        return NadelResultInstruction.Set(
            subjectPath = parentNode.resultPath + state.hydratedField.resultKey,
            newValue = newValue,
        )
    }

    /**
     * For the following example
     *
     * ```
     * type Issue {
     *   details: IssueDetails
     *   owner: User @hydrated(from: ["issueOwner"], args: [
     *     {name: "issueId" valueFromField: ["details", "authorId"]}
     *   ])
     * }
     *
     * type IssueDetails {
     *   authorId: ID!
     * }
     * ```
     *
     * We are getting the path `["details", "authorId"]`
     */
    private fun getPathToObjectIdentifierOnHydrationParentNode(
        instruction: NadelBatchHydrationFieldInstruction,
    ): NadelQueryPath {
        return instruction
            .actorInputValueDefs
            .asSequence()
            .map { it.valueSource }
            .filterIsInstance<NadelHydrationActorInputDef.ValueSource.FieldResultValue>()
            .single()
            .queryPathToField
    }
}
