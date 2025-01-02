// @formatter:off
package graphql.nadel.tests.legacy.errors

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`exceptions in service execution result completable future in graphql errors`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `exceptions in service execution result completable future in graphql errors snapshot`
        : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            )

    /**
     * ```json
     * {
     *   "errors": [
     *     {
     *       "message": "An exception occurred invoking the service 'MyService': Pop goes the
     * weasel",
     *       "locations": [],
     *       "extensions": {
     *         "executionId": "test",
     *         "classification": "DataFetchingException"
     *       }
     *     }
     *   ],
     *   "data": {
     *     "hello": null
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "errors": [
            |     {
            |       "message": "An exception occurred invoking the service 'MyService': Pop goes the weasel",
            |       "locations": [],
            |       "extensions": {
            |         "executionId": "test",
            |         "classification": "DataFetchingException"
            |       }
            |     }
            |   ],
            |   "data": {
            |     "hello": null
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
