package graphql.nadel.engine.transform.hydration

import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.transform.hydration.batch.NadelResolvedObjectBatch
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.util.toGraphQLError

internal object NadelHydrationUtil {
    @JvmName("getInstructionsToAddErrors_2")
    fun getInstructionsToAddErrors(
        results: List<NadelResolvedObjectBatch>,
    ): List<NadelResultInstruction> {
        return results
            .asSequence()
            .map(NadelResolvedObjectBatch::result)
            .flatMap(::sequenceOfInstructionsToAddErrors)
            .toList()
    }

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
}
