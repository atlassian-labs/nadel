package graphql.nadel.engine.transform.hydration

import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodeExtractor
import graphql.nadel.engine.util.emptyOrSingle
import graphql.nadel.engine.util.toGraphQLError

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
}
