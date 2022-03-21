package graphql.nadel.engine.util

/**
 * Read the source code, it's a lot easier to understand than words.
 *
 * Effectively though, this function lets you transform the [initial] object
 * via the [transform] function [count] times.
 *
 * So instead of doing something like
 *
 * ```kotlin
 * val field: ExecutableNormalizedField
 * field.parent.parent
 * ```
 *
 * you can do
 *
 * ```kotlin
 * val field: ExecutableNormalizedField
 * fold(initial = field, count = 2) {
 *     it.parent
 * }
 * ```
 *
 * But with the ability to call `.parent` an arbitrary amount of times depending on [count].
 *
 * This is very similar to
 *
 * ```kotlin
 * arrayOfNulls<Unit>(size = 2).fold(field) { it, _ ->
 *   it.parent
 * }
 * ```
 *
 * which is why it is named [fold].
 */
fun <T> fold(initial: T, count: Int, transform: (T) -> T): T {
    var element = initial
    for (i in 1..count) {
        element = transform(element)
    }
    return element
}
