package graphql.nadel.validation

import graphql.nadel.validation.NadelSchemaValidationResults.Companion.flatten
import graphql.nadel.validation.NadelValidationInterimResult.Error
import graphql.nadel.validation.NadelValidationInterimResult.Success

internal sealed interface NadelValidationInterimResult<T> {
    data class Success<T> private constructor(
        val data: T,
    ) : NadelValidationInterimResult<T> {
        companion object {
            context(NadelValidationContext)
            internal fun <T : Any?> T.asInterimSuccess(): Success<T> {
                return of(value = this)
            }

            internal fun <T : Any?> of(value: T): Success<T> {
                return Success(value)
            }
        }
    }

    data class Error<T> private constructor(
        override val results: List<NadelSchemaValidationResult>,
    ) : NadelValidationInterimResult<T>, NadelSchemaValidationResults {
        override val isError: Boolean = true

        companion object {
            context(NadelSchemaValidationOnErrorContext)
            internal fun <T : Any> NadelSchemaValidationResult.asInterimError(): Error<T> {
                return of(this)
            }

            context(NadelValidationContext)
            internal fun <T : Any> NadelSchemaValidationResult.asInterimError(): Error<T> {
                return of(this)
            }

            context(NadelValidationContext)
            internal fun <T : Any> List<NadelSchemaValidationError>.asInterimError(): Error<T> {
                return of(value = this)
            }

            internal fun <T : Any> of(value: NadelSchemaValidationResult): Error<T> {
                return Error(flatten(value))
            }

            internal fun <T : Any> of(value: List<NadelSchemaValidationError>): Error<T> {
                return Error(flatten(value))
            }
        }
    }
}

internal inline fun <T> NadelValidationInterimResult<T>.onError(
    onError: (Error<T>) -> Nothing,
): T {
    when (this) {
        is Error -> onError(this)
        is Success -> return data
    }
}

internal inline fun <T, E> NadelValidationInterimResult<T>.onErrorCast(
    onError: (Error<E>) -> Nothing,
): T {
    when (this) {
        is Error -> {
            @Suppress("UNCHECKED_CAST") // Generics don't matter for errors
            onError(this as Error<E>)
        }
        is Success -> return data
    }
}
