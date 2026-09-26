// @formatter:off
package graphql.nadel.tests.next.fixtures.instrumentation

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<InstrumentationBeginExecuteOnCompleteOnFailureTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class InstrumentationBeginExecuteOnCompleteOnFailureTestSnapshot : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * query {
     *   echo
     * }
     * ```
     *
     * Variables
     *
     * ```json
     * {}
     * ```
     */
    override val calls: List<ExpectedServiceCall> = listOf(
            )

    /**
     * ```json
     * {
     *   "errors": [
     *     {
     *       "message": "An UnsupportedOperationException occurred invoking the service tester",
     *       "locations": [],
     *       "extensions": {
     *         "executionId": "stable-id",
     *         "classification": "NadelUncaughtExecutionError"
     *       }
     *     }
     *   ],
     *   "data": {
     *     "echo": null
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "errors": [
            |     {
            |       "message": "An UnsupportedOperationException occurred invoking the service tester",
            |       "locations": [],
            |       "extensions": {
            |         "executionId": "stable-id",
            |         "classification": "NadelUncaughtExecutionError"
            |       }
            |     }
            |   ],
            |   "data": {
            |     "echo": null
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
