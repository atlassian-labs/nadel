package graphql.nadel.validation.util

import graphql.nadel.engine.util.singleOfTypeOrNull
import graphql.nadel.validation.NadelSchemaValidationError

/**
 * Provides better error message when it fails.
 */
inline fun <reified T : NadelSchemaValidationError> Set<NadelSchemaValidationError>.assertSingleOfType(): T {
    val errors = this
    val singleOrNull = singleOfTypeOrNull<T>()

    return singleOrNull
        ?: run {
            val className = T::class.java.simpleName
            val classNamesFound = errors.map { it.javaClass.name }
            error("Could not find single error of type '$className' but found: $classNamesFound")
        }
}

/**
 * Provides better error message when it fails.
 */
inline fun <reified T : NadelSchemaValidationError> Set<NadelSchemaValidationError>.assertSingleOfType(
    crossinline predicate: (T) -> Boolean,
): T {
    val errors = this
    val singleOrNull = singleOfTypeOrNull(predicate)

    return singleOrNull
        ?: run {
            val className = T::class.java.simpleName
            val errorsFound = errors.map { it.message }
            error("Could not find single error of type '$className' matching predicate but found: $errorsFound")
        }
}
