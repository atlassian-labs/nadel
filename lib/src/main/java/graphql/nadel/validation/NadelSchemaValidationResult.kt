package graphql.nadel.validation

import graphql.nadel.Service
import graphql.nadel.engine.blueprint.NadelFieldInstruction
import graphql.nadel.engine.blueprint.NadelTypeRenameInstruction
import graphql.nadel.validation.NadelValidationInterimResult.Error.Companion.asInterimError

sealed interface NadelSchemaValidationResult {
    val isError: Boolean
}

internal sealed interface NadelServiceValidationResult {
    val service: Service
}

internal interface NadelSchemaValidationResults : NadelSchemaValidationResult {
    val results: List<NadelSchemaValidationResult>

    override val isError: Boolean
        get() = results.any {
            it.isError
        }

    companion object {
        operator fun invoke(
            results: List<NadelSchemaValidationResult>,
        ): NadelSchemaValidationResult {
            return Impl(flatten(results))
        }

        internal fun flatten(result: List<NadelSchemaValidationResult>): List<NadelSchemaValidationResult> {
            return result.flatMap(::flatten)
        }

        internal fun flatten(result: NadelSchemaValidationResult): List<NadelSchemaValidationResult> {
            return if (result is NadelSchemaValidationResults) {
                result.results
            } else {
                listOf(result)
            }
        }
    }

    private data class Impl(
        override val results: List<NadelSchemaValidationResult>,
    ) : NadelSchemaValidationResults
}

internal object NadelSchemaValidationEmptyResult : NadelSchemaValidationResults {
    override val results: List<NadelSchemaValidationResult>
        get() = emptyList()
}

context(NadelValidationContext)
internal fun ok(): NadelSchemaValidationResult {
    return NadelSchemaValidationEmptyResult
}

context(NadelValidationContext)
internal fun results(vararg results: NadelSchemaValidationResult): NadelSchemaValidationResult {
    return NadelSchemaValidationResults(results.toList())
}

context(NadelValidationContext)
fun Sequence<NadelSchemaValidationResult>.toResult(): NadelSchemaValidationResult {
    return NadelSchemaValidationResults(toList())
}

context(NadelValidationContext)
fun List<NadelSchemaValidationResult>.toResult(): NadelSchemaValidationResult {
    return NadelSchemaValidationResults(this)
}

internal class NadelValidatedTypeResult(
    val typeRenameInstruction: NadelTypeRenameInstruction,
) : NadelSchemaValidationResult, NadelServiceValidationResult {
    override val service: Service get() = typeRenameInstruction.service
    override val isError: Boolean = false
}

internal class NadelReachableServiceTypesResult(
    override val service: Service,
    val underlyingTypeNames: Set<String>,
    val overallTypeNames: Set<String>,
) : NadelSchemaValidationResult, NadelServiceValidationResult {
    override val isError: Boolean = false
}

internal class NadelValidatedFieldResult(
    override val service: Service,
    val fieldInstruction: NadelFieldInstruction,
) : NadelSchemaValidationResult, NadelServiceValidationResult {
    override val isError: Boolean = false
}

internal object NadelSchemaValidationOnErrorContext

internal inline fun List<NadelSchemaValidationResult>.onError(
    onError: NadelSchemaValidationOnErrorContext.(NadelSchemaValidationResult) -> Nothing,
): NadelSchemaValidationResult {
    val results = NadelSchemaValidationResults(this)

    if (any(NadelSchemaValidationResult::isError)) {
        with(NadelSchemaValidationOnErrorContext) {
            onError(results)
        }
    }

    return results
}

internal inline fun <T : NadelSchemaValidationResult?> T.onError(
    onError: NadelSchemaValidationOnErrorContext.(NadelSchemaValidationResult) -> Nothing,
): T {
    if (this?.isError == true) {
        with(NadelSchemaValidationOnErrorContext) {
            onError(this@onError)
        }
    }

    return this
}

internal inline fun <T : Any> NadelSchemaValidationResult.onErrorReturnInterim(
    onError: NadelSchemaValidationOnErrorContext.(NadelValidationInterimResult.Error<T>) -> Nothing,
): NadelSchemaValidationResult {
    onError {
        onError(it.asInterimError())
    }

    return this
}
