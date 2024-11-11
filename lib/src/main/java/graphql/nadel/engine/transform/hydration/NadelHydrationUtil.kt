package graphql.nadel.engine.transform.hydration

import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.transform.hydration.batch.NadelResolvedObjectBatch
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.util.toGraphQLError

internal object NadelHydrationUtil {
    @JvmName("getInstructionsToAddErrors_2")
    fun getInstructionsToAddErrors(
        results: List<NadelResolvedObjectBatch>,
    ): List<NadelResultInstruction.AddError> {
        return results
            .asSequence()
            .map(NadelResolvedObjectBatch::result)
            .flatMap(::getInstructionsToAddErrorsSequence)
            .toList()
    }

    fun getInstructionsToAddErrors(
        results: List<ServiceExecutionResult>,
    ): List<NadelResultInstruction.AddError> {
        return results
            .asSequence()
            .flatMap(::getInstructionsToAddErrorsSequence)
            .toList()
    }

    fun getInstructionsToAddErrors(
        result: ServiceExecutionResult,
    ): List<NadelResultInstruction.AddError> {
        return getInstructionsToAddErrorsSequence(result).toList()
    }

    /**
     * Do not expose sequences as those
     */
    private fun getInstructionsToAddErrorsSequence(
        result: ServiceExecutionResult,
    ): Sequence<NadelResultInstruction.AddError> {
        return result.errors
            .asSequence()
            .map(::toGraphQLError)
            .map(NadelResultInstruction::AddError)
    }
}
