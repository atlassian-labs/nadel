package graphql.nadel.enginekt.transform.hydration

import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.enginekt.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.enginekt.transform.artificial.NadelAliasHelper
import graphql.nadel.enginekt.transform.hydration.batch.NadelBatchHydrationInputBuilder
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.util.emptyOrSingle
import graphql.nadel.enginekt.util.toGraphQLError

internal object NadelHydrationUtil {
    fun getInstructionsToAddErrors(
        results: List<ServiceExecutionResult>,
    ): List<NadelResultInstruction> {
        return results
            .asSequence()
            .flatMap(::sequenceOfInstructionsToAddErrors)
            .toList()
    }

    fun getInstructionsToAddErrors(
        result: ServiceExecutionResult,
    ): List<NadelResultInstruction> {
        return sequenceOfInstructionsToAddErrors(result).toList()
    }

    /**
     * Do not expose sequences as those
     */
    private fun sequenceOfInstructionsToAddErrors(
        result: ServiceExecutionResult,
    ): Sequence<NadelResultInstruction> {
        return result.errors
            .asSequence()
            .map(::toGraphQLError)
            .map(NadelResultInstruction::AddError)
    }

    fun getHydrationActorNodes(
        instruction: NadelGenericHydrationInstruction,
        batches: List<ServiceExecutionResult>,
    ): List<JsonNode> {
        return batches
            .asSequence()
            .mapNotNull { batch ->
                getHydrationActorNode(instruction, batch)
            }
            .toList()
    }

    fun getHydrationActorNode(
        instruction: NadelGenericHydrationInstruction,
        batch: ServiceExecutionResult,
    ): JsonNode? {
        return JsonNodeExtractor.getNodesAt(
            data = batch.data ?: return null,
            queryPath = instruction.queryPathToActorField,
        ).emptyOrSingle()
    }

    fun getNumberOfInputNodes(
        aliasHelper: NadelAliasHelper,
        instruction: NadelBatchHydrationFieldInstruction,
        parentNode: JsonNode,
    ): Int {
        val (_, batchInputValueSource) = NadelBatchHydrationInputBuilder.getBatchInputDef(instruction)
            ?: error("Batch hydration is missing batch input arg") // TODO: we should bake this into the instruction

        return NadelBatchHydrationInputBuilder.getFieldResultValues(
            valueSource = batchInputValueSource,
            parentNode = parentNode,
            aliasHelper = aliasHelper,
        ).size
    }
}
