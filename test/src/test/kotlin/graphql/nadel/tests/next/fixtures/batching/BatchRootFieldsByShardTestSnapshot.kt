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
    graphql.nadel.tests.next.update<BatchRootFieldsByShardTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class BatchRootFieldsByShardTestSnapshot : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * query {
     *   a: issueById(cloudId: "site-1") { id }
     *   b: issueById(cloudId: "site-1") { id }
     *   c: issueById(cloudId: "site-2") { id }
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
            ExpectedServiceCall(
                service = "issues",
                query = """
                | {
                |   a: issueById(cloudId: "site-1") {
                |     id
                |   }
                |   b: issueById(cloudId: "site-1") {
                |     id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "a": {
                |       "id": "site-1"
                |     },
                |     "b": {
                |       "id": "site-1"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "issues",
                query = """
                | {
                |   c: issueById(cloudId: "site-2") {
                |     id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "c": {
                |       "id": "site-2"
                |     }
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
     *     "a": {
     *       "id": "site-1"
     *     },
     *     "b": {
     *       "id": "site-1"
     *     },
     *     "c": {
     *       "id": "site-2"
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "a": {
            |       "id": "site-1"
            |     },
            |     "b": {
            |       "id": "site-1"
            |     },
            |     "c": {
            |       "id": "site-2"
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
