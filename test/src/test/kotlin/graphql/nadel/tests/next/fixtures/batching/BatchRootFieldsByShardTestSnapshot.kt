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
                | query (${'$'}v0: String!, ${'$'}v1: String!) {
                |   a: issueById(cloudId: ${'$'}v0) {
                |     id
                |   }
                |   b: issueById(cloudId: ${'$'}v1) {
                |     id
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "site-1",
                |   "v1": "site-1"
                | }
                """.trimMargin(),
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
                | query (${'$'}v0: String!) {
                |   c: issueById(cloudId: ${'$'}v0) {
                |     id
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "site-2"
                | }
                """.trimMargin(),
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
