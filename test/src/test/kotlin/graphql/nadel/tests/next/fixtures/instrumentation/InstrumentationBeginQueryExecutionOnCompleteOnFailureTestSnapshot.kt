// @formatter:off
package graphql.nadel.tests.next.fixtures.instrumentation

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

public suspend fun main() {
    graphql.nadel.tests.next.update<InstrumentationBeginQueryExecutionOnCompleteOnFailureTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class InstrumentationBeginQueryExecutionOnCompleteOnFailureTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            )

    /**
     * ```json
     * {
     *   "errors": [
     *     {
     *       "message": "An exception occurred invoking the service 'tester': no-op",
     *       "locations": [],
     *       "extensions": {
     *         "executionId": "stable-id",
     *         "classification": "DataFetchingException"
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
            |       "message": "An exception occurred invoking the service 'tester': no-op",
            |       "locations": [],
            |       "extensions": {
            |         "executionId": "stable-id",
            |         "classification": "DataFetchingException"
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
