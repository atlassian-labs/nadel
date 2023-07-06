package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.NadelEngineContext
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.transform.NadelTransformUtil
import graphql.nadel.engine.transform.hydration.NadelHydrationTransformContext
import graphql.nadel.engine.transform.hydration.NadelHydrationUtil.getHydrationEffectNodes
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationTransform.NadelBatchHydrationContext
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.NadelResultKey
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
    context(NadelEngineContext, NadelBatchHydrationContext)
    fun getHydrateInstructionsMatchingObjectIds(
        instruction: NadelBatchHydrationFieldInstruction,
        parentNodes: List<JsonNode>,
        batches: List<ServiceExecutionResult>,
        matchStrategy: NadelBatchHydrationMatchStrategy.MatchObjectIdentifiers,
    ): List<NadelResultInstruction> {
        return getHydrateInstructionsMatchingObjectIds(
            instruction,
            parentNodes,
            batches,
            matchStrategy.objectIds,
        )
    }

    context(NadelEngineContext, NadelBatchHydrationContext)
    fun getHydrateInstructionsMatchingObjectId(
        instruction: NadelBatchHydrationFieldInstruction,
        parentNodes: List<JsonNode>,
        batches: List<ServiceExecutionResult>,
        matchStrategy: NadelBatchHydrationMatchStrategy.MatchObjectIdentifier,
    ): List<NadelResultInstruction> {
        return getHydrateInstructionsMatchingObjectIds(
            instruction,
            parentNodes,
            batches,
            listOf(matchStrategy),
        )
    }

    context(NadelEngineContext, NadelBatchHydrationContext)
    private fun getHydrateInstructionsMatchingObjectIds(
        instruction: NadelBatchHydrationFieldInstruction,
        parentNodes: List<JsonNode>,
        batches: List<ServiceExecutionResult>,
        objectIds: List<NadelBatchHydrationMatchStrategy.MatchObjectIdentifier>,
    ): List<NadelResultInstruction> {
        // Associate by does not need to be strict here
        val resultNodesByObjectId = getHydrationEffectNodes(instruction, batches)
            .asSequence()
            .map(JsonNode::value)
            .flatten(recursively = true)
            .mapNotNull {
                when (it) {
                    is AnyMap? -> it.asNullableJsonMap()
                    else -> error("Hydration effect result must be an object")
                }
            }
            .associateBy { resultNode ->
                objectIds
                    .map { objectId ->
                        // We don't want to show this in the overall result, so remove it here as we use it
                        resultNode.asMutable().remove(aliasHelper.getResultKey(objectId.resultId))
                    }
            }

        val pathsToSourceIds = objectIds
            .map { objectId ->
                aliasHelper.getQueryPath(objectId.sourceId)
            }

        return parentNodes.map { sourceNode ->
            // [
            //   [page-1, page-2, page-1], // page id
            //   [draft,  posted, posted], // union of [draft posted]
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
            //   [page-1, posted],
            // ]
            val sourceIds = (0..sourceIdNodes.first().lastIndex)
                .map { column ->
                    sourceIdNodes // row
                        .map {
                            it[column]
                        }
                }
                .flatMap { id ->
                    if (sourceIdNodes.size == 1) {
                        hydrationIdMapping?.get(id.single())?.map { listOf(it) } ?: listOf(id)
                    } else {
                        listOf(id)
                    }
                }

            getHydrateInstructionsForNodeMatchingObjectId(
                sourceNode = sourceNode,
                sourceIds = sourceIds,
                resultNodesByObjectId = resultNodesByObjectId
            )
        }
    }

    context(NadelEngineContext, NadelHydrationTransformContext)
    private fun getHydrateInstructionsForNodeMatchingObjectId(
        sourceNode: JsonNode,
        sourceIds: List<List<Any?>>,
        resultNodesByObjectId: Map<List<Any?>, JsonMap>,
    ): NadelResultInstruction {
        val causeFieldDef = NadelTransformUtil.getOverallFieldDef(
            overallField = hydrationCauseField,
            parentNode = sourceNode,
            service = hydrationCauseService,
            executionBlueprint = executionBlueprint,
            aliasHelper = aliasHelper,
        ) ?: error("Unable to find field definition for ${hydrationCauseField.queryPath}")

        val newValue: Any? = if (causeFieldDef.type.unwrapNonNull().isList) {
            // Set to null if there were no identifier nodes
            if (isAllNull(sourceIds)) {
                null
            } else {
                sourceIds
                    .map { id ->
                        resultNodesByObjectId[id]
                    }
            }
        } else {
            sourceIds.emptyOrSingle()?.let { sourceId ->
                resultNodesByObjectId[sourceId]
            }
        }

        return NadelResultInstruction.Set(
            subject = sourceNode,
            key = NadelResultKey(hydrationCauseField.resultKey),
            newValue = JsonNode(newValue),
        )
    }

    private fun isAllNull(sourceIds: List<List<Any?>>): Boolean {
        return sourceIds.isNotEmpty() && sourceIds.all { compositeId ->
            compositeId.all { it == null }
        }
    }
}
