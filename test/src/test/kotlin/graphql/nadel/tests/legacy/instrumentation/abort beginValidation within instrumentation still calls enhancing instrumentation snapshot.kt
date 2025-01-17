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
    graphql.nadel.tests.next.update<`abort beginValidation within instrumentation still calls enhancing instrumentation`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class
        `abort beginValidation within instrumentation still calls enhancing instrumentation snapshot`
        : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            )

    /**
     * ```json
     * {
     *   "errors": [
     *     {
     *       "message": "beginValidation",
     *       "extensions": {
     *         "classification": "ExecutionAborted"
     *       }
     *     }
     *   ],
     *   "data": {
     *     "step": "beginValidation"
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "errors": [
            |     {
            |       "message": "beginValidation",
            |       "extensions": {
            |         "classification": "ExecutionAborted"
            |       }
            |     }
            |   ],
            |   "data": {
            |     "step": "beginValidation"
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
