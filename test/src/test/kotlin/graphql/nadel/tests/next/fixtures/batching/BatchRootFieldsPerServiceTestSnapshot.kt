// @formatter:off
package graphql.nadel.tests.next.fixtures.batching

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<BatchRootFieldsPerServiceTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class BatchRootFieldsPerServiceTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "batched",
                query = """
                | {
                |   batchedBar
                |   batchedFoo
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "batchedFoo": "batchedFoo-value",
                |     "batchedBar": "batchedBar-value"
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "unbatched",
                query = """
                | {
                |   unbatchedBar
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "unbatchedBar": "unbatchedBar-value"
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "unbatched",
                query = """
                | {
                |   unbatchedFoo
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "unbatchedFoo": "unbatchedFoo-value"
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
        )

    /**
     * ```json
     * {
     *   "data": {
     *     "unbatchedFoo": "unbatchedFoo-value",
     *     "unbatchedBar": "unbatchedBar-value",
     *     "batchedFoo": "batchedFoo-value",
     *     "batchedBar": "batchedBar-value"
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "unbatchedFoo": "unbatchedFoo-value",
            |     "unbatchedBar": "unbatchedBar-value",
            |     "batchedFoo": "batchedFoo-value",
            |     "batchedBar": "batchedBar-value"
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
