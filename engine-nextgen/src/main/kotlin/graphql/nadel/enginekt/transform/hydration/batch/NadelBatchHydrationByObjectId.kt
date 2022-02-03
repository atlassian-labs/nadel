package graphql.nadel.enginekt.transform.hydration.batch

import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.enginekt.transform.NadelTransformUtil
import graphql.nadel.enginekt.transform.hydration.NadelHydrationUtil.getHydrationActorNodes
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.asMutable
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.util.AnyMap
import graphql.nadel.enginekt.util.JsonMap
import graphql.nadel.enginekt.util.asNullableJsonMap
import graphql.nadel.enginekt.util.emptyOrSingle
import graphql.nadel.enginekt.util.flatten
import graphql.nadel.enginekt.util.isList
import graphql.nadel.enginekt.util.queryPath
import graphql.nadel.enginekt.util.unwrapNonNull

internal object NadelBatchHydrationByObjectId {
    fun getHydrateInstructionsMatchingObjectIds(
        executionBlueprint: NadelOverallExecutionBlueprint,
        state: NadelBatchHydrationTransform.State,
        instruction: NadelBatchHydrationFieldInstruction,
        parentNodes: List<JsonNode>,
        batches: List<ServiceExecutionResult>,
        matchStrategy: NadelBatchHydrationMatchStrategy.MatchObjectIdentifiers,
    ): List<NadelResultInstruction> {
        return getHydrateInstructionsMatchingObjectIds(
            executionBlueprint,
            state,
            instruction,
            parentNodes,
            batches,
            matchStrategy.objectIds,
        )
    }

    fun getHydrateInstructionsMatchingObjectId(
        executionBlueprint: NadelOverallExecutionBlueprint,
        state: NadelBatchHydrationTransform.State,
        instruction: NadelBatchHydrationFieldInstruction,
        parentNodes: List<JsonNode>,
        batches: List<ServiceExecutionResult>,
        matchStrategy: NadelBatchHydrationMatchStrategy.MatchObjectIdentifier,
    ): List<NadelResultInstruction> {
        return getHydrateInstructionsMatchingObjectIds(
            executionBlueprint,
            state,
            instruction,
            parentNodes,
            batches,
            listOf(matchStrategy),
        )
    }

    private fun getHydrateInstructionsMatchingObjectIds(
        executionBlueprint: NadelOverallExecutionBlueprint,
        state: NadelBatchHydrationTransform.State,
        instruction: NadelBatchHydrationFieldInstruction,
        parentNodes: List<JsonNode>,
        batches: List<ServiceExecutionResult>,
        objectIds: List<NadelBatchHydrationMatchStrategy.MatchObjectIdentifier>,
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
            .associateBy { resultNode ->
                objectIds
                    .map { objectId ->
                        // We don't want to show this in the overall result, so remove it here as we use it
                        resultNode.asMutable().remove(state.aliasHelper.getResultKey(objectId.resultId))
                    }
            }

        val pathsToSourceIds = objectIds
            .map { objectId ->
                state.aliasHelper.getQueryPath(objectId.sourceId)
            }

        return parentNodes.map { sourceNode ->
            // [
            //   [page-1, page-2, page-1], // page id
            //   [draft,  posted, posted] // union of [draft posted]
            // ]
            val sourceIdNodes = pathsToSourceIds
                .map { path ->
                    JsonNodeExtractor.getNodesAt(sourceNode, path)
                        .asSequence()
                        .map(JsonNode::value)
                        .flatten(recursively = true)
                        .toList()
                }

            val sourceIds = (0 until sourceIdNodes.first().size)
                .map { keyIndex ->
                    sourceIdNodes
                        .map {
                            it[keyIndex]
                        }
                }

            getHydrateInstructionsForNodeMatchingObjectId(
                executionBlueprint = executionBlueprint,
                state = state,
                sourceNode = sourceNode,
                sourceIds = sourceIds,
                resultNodesByObjectId = resultNodesByObjectId
            )
        }
    }

    private fun getHydrateInstructionsForNodeMatchingObjectId(
        executionBlueprint: NadelOverallExecutionBlueprint,
        state: NadelBatchHydrationTransform.State,
        sourceNode: JsonNode,
        sourceIds: List<List<Any?>>,
        resultNodesByObjectId: Map<List<Any?>, JsonMap>,
    ): NadelResultInstruction {
        val hydratedFieldDef = NadelTransformUtil.getOverallFieldDef(
            overallField = state.hydratedField,
            parentNode = sourceNode,
            service = state.hydratedFieldService,
            executionBlueprint = executionBlueprint,
            aliasHelper = state.aliasHelper,
        ) ?: error("Unable to find field definition for ${state.hydratedField.queryPath}")

        val newValue: Any? = if (hydratedFieldDef.type.unwrapNonNull().isList) {
            // Set to null if there were no identifier nodes
            if (sourceIds.isNotEmpty() && sourceIds.all { it.all { it == null } }) {
                null
            } else {
                sourceIds
                    .asSequence()
                    .map { it }
                    // .flatten(recursively = true)
                    // .filterNotNull()
                    .map { id ->
                        resultNodesByObjectId[id]
                    }
                    .toList()
            }
        } else {
            sourceIds.emptyOrSingle()?.let { sourceId ->
                resultNodesByObjectId[sourceId]
            }
        }

        return NadelResultInstruction.Set(
            subjectPath = sourceNode.resultPath + state.hydratedField.resultKey,
            newValue = newValue,
        )
    }
}
