package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.blueprint.hydration.NadelHydrationActorInputDef
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationInputBuilder.getBatchInputDef
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationInputBuilder.getNonBatchInputValues
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.util.javaValueToAstValue
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue
import graphql.schema.GraphQLTypeUtil

/**
 * todo: did I follow this README when forking?
 *
 * README
 *
 * Please ensure that the batch arguments are ordered according to the input.
 * This is required for [NadelBatchHydrationMatchStrategy.MatchIndex].
 */
internal object NadelNewBatchHydrationInputBuilder {
    fun getInputValueBatches(
        hooks: NadelExecutionHooks,
        userContext: Any?,
        instruction: NadelBatchHydrationFieldInstruction,
        hydrationField: ExecutableNormalizedField,
        sourceIds: List<JsonNode>,
    ): List<Map<NadelHydrationActorInputDef, NormalizedInputValue>> {
        val nonBatchArgs = getNonBatchInputValues(instruction, hydrationField)
        val batchArgs = getBatchInputValues(instruction, sourceIds, hooks, userContext)

        return batchArgs.map { nonBatchArgs + it }
    }

    private fun getBatchInputValues(
        instruction: NadelBatchHydrationFieldInstruction,
        sourceIds: List<JsonNode>,
        hooks: NadelExecutionHooks,
        userContext: Any?,
    ): List<Pair<NadelHydrationActorInputDef, NormalizedInputValue>> {
        val batchSize = instruction.batchSize

        val (batchInputDef, batchInputValueSource) = getBatchInputDef(instruction) ?: return emptyList()
        val actorBatchArgDef = instruction.actorFieldDef.getArgument(batchInputDef.name)

        val partitionArgumentList = hooks.partitionBatchHydrationArgumentList(
            argumentValues = sourceIds.map { it.value },
            instruction = instruction,
            userContext = userContext,
        )

        return partitionArgumentList
            .flatMap {
                it.chunked(size = batchSize)
            }
            .map { chunk ->
                batchInputDef to NormalizedInputValue(
                    GraphQLTypeUtil.simplePrint(actorBatchArgDef.type),
                    javaValueToAstValue(chunk),
                )
            }
    }
}
