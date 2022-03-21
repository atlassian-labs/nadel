package graphql.nadel.test

/**
 * Util function to debug e.g.
 *
 * ```kotlin
 * fun main() {
 *   println(dbg(1 + 2 + 3))
 * }
 * ```
 *
 * prints the following
 *
 * ```
 * dbg(1 + 2 + 3)
 *       |   |
 *       |   6
 *       3
 * 6
 * ```
 */
fun <T> dbg(value: T): T = value
