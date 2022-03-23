package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.transform.NadelTransformUtil
import graphql.nadel.engine.transform.hydration.NadelHydrationUtil.getHydrationActorNodes
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.asMutable
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodeExtractor
import graphql.nadel.engine.util.AnyMap
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.asNullableJsonMap
import graphql.nadel.engine.util.emptyOrSingle
import graphql.nadel.engine.util.flatten
import graphql.nadel.engine.util.isList
import graphql.nadel.engine.util.queryPath
import graphql.nadel.engine.util.unwrapNonNull

internal object NadelBatchHydrationByObjectId {
    fun getHydrateInstructionsMatchingObjectIds(
        state: NadelBatchHydrationTransform.State,
        instruction: NadelBatchHydrationFieldInstruction,
        parentNodes: List<JsonNode>,
        batches: List<ServiceExecutionResult>,
        matchStrategy: NadelBatchHydrationMatchStrategy.MatchObjectIdentifiers,
    ): List<NadelResultInstruction> {
        return getHydrateInstructionsMatchingObjectIds(
            state,
            instruction,
            parentNodes,
            batches,
            matchStrategy.objectIds,
        )
    }

    fun getHydrateInstructionsMatchingObjectId(
        state: NadelBatchHydrationTransform.State,
        instruction: NadelBatchHydrationFieldInstruction,
        parentNodes: List<JsonNode>,
        batches: List<ServiceExecutionResult>,
        matchStrategy: NadelBatchHydrationMatchStrategy.MatchObjectIdentifier,
    ): List<NadelResultInstruction> {
        return getHydrateInstructionsMatchingObjectIds(
            state,
            instruction,
            parentNodes,
            batches,
            listOf(matchStrategy),
        )
    }

    private fun getHydrateInstructionsMatchingObjectIds(
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

            // Turns sourceIdNodes into
            // [
            //   [page-1, draft],
            //   [page-2, posted],
            //   [page-1, posted]
            // ]
            val sourceIds = (0 until sourceIdNodes.first().size)
                .map { keyIndex ->
                    sourceIdNodes
                        .map {
                            it[keyIndex]
                        }
                }

            getHydrateInstructionsForNodeMatchingObjectId(
                state = state,
                sourceNode = sourceNode,
                sourceIds = sourceIds,
                resultNodesByObjectId = resultNodesByObjectId
            )
        }
    }

    private fun getHydrateInstructionsForNodeMatchingObjectId(
        state: NadelBatchHydrationTransform.State,
        sourceNode: JsonNode,
        sourceIds: List<List<Any?>>,
        resultNodesByObjectId: Map<List<Any?>, JsonMap>,
    ): NadelResultInstruction {
        val hydratedFieldDef = NadelTransformUtil.getOverallFieldDef(
            overallField = state.hydratedField,
            parentNode = sourceNode,
            service = state.hydratedFieldService,
            aliasHelper = state.aliasHelper,
        ) ?: error("Unable to find field definition for ${state.hydratedField.queryPath}")

        val newValue: Any? = if (hydratedFieldDef.type.unwrapNonNull().isList) {
            // Set to null if there were no identifier nodes
            if (isAllNull(sourceIds)) {
                null
            } else {
                sourceIds
                    .asSequence()
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

    private fun isAllNull(sourceIds: List<List<Any?>>): Boolean {
        return sourceIds.isNotEmpty() && sourceIds.all { compositeId ->
            compositeId.all { it == null }
        }
    }
}
