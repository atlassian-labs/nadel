// @formatter:off
package graphql.nadel.tests.legacy.instrumentation

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`execution is aborted when beginExecute completes exceptionally using chained instrumentation`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class
        `execution is aborted when beginExecute completes exceptionally using chained instrumentation snapshot`
        : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            )

    /**
     * ```json
     * {
     *   "errors": [
     *     {
     *       "message": "instrumented-error",
     *       "extensions": {
     *         "classification": "ExecutionAborted"
     *       }
     *     }
     *   ],
     *   "data": null
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "errors": [
            |     {
            |       "message": "instrumented-error",
            |       "extensions": {
            |         "classification": "ExecutionAborted"
            |       }
            |     }
            |   ],
            |   "data": null
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
