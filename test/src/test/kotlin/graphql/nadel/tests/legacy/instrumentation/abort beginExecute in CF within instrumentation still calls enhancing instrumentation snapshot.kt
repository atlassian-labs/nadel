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
    graphql.nadel.tests.next.update<`abort beginExecute in CF within instrumentation still calls enhancing instrumentation`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class
        `abort beginExecute in CF within instrumentation still calls enhancing instrumentation snapshot`
        : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            )

    /**
     * ```json
     * {
     *   "data": {
     *     "step": "beginExecute"
     *   },
     *   "errors": [
     *     {
     *       "message": "beginExecute",
     *       "extensions": {
     *         "classification": "ExecutionAborted"
     *       }
     *     }
     *   ]
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "step": "beginExecute"
            |   },
            |   "errors": [
            |     {
            |       "message": "beginExecute",
            |       "extensions": {
            |         "classification": "ExecutionAborted"
            |       }
            |     }
            |   ]
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
