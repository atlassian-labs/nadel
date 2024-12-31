// @formatter:off
package graphql.nadel.tests.legacy.oneOf

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`oneOf fails when no values are passed`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `oneOf fails when no values are passed snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            )

    /**
     * ```json
     * {
     *   "errors": [
     *     {
     *       "message": "Validation error (WrongType@[search]) : Exactly one key must be specified
     * for OneOf type 'SearchInput'.",
     *       "locations": [
     *         {
     *           "line": 2,
     *           "column": 10
     *         }
     *       ],
     *       "extensions": {
     *         "classification": "ValidationError"
     *       }
     *     }
     *   ]
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "errors": [
            |     {
            |       "message": "Validation error (WrongType@[search]) : Exactly one key must be specified for OneOf type 'SearchInput'.",
            |       "locations": [
            |         {
            |           "line": 2,
            |           "column": 10
            |         }
            |       ],
            |       "extensions": {
            |         "classification": "ValidationError"
            |       }
            |     }
            |   ]
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
