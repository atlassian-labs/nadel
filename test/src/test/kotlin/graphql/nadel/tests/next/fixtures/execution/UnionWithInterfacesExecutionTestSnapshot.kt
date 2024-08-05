// @formatter:off
package graphql.nadel.tests.next.fixtures.execution

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<UnionWithInterfacesExecutionTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class UnionWithInterfacesExecutionTestSnapshot : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * query {
     *   union {
     *     __typename
     *     ... on Interface {
     *       id
     *     }
     *     ... on User {
     *       name
     *     }
     *   }
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
                service = "abstract",
                query = """
                | {
                |   union {
                |     __typename
                |     ... on User {
                |       id
                |       name
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "union": [
                |       {
                |         "__typename": "User",
                |         "id": "user/1",
                |         "name": "Hello"
                |       },
                |       {
                |         "__typename": "Issue"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
        )

    /**
     * Combined Result
     *
     * ```json
     * {
     *   "data": {
     *     "union": [
     *       {
     *         "__typename": "User",
     *         "id": "user/1",
     *         "name": "Hello"
     *       },
     *       {
     *         "__typename": "Issue"
     *       }
     *     ]
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "union": [
            |       {
            |         "__typename": "User",
            |         "id": "user/1",
            |         "name": "Hello"
            |       },
            |       {
            |         "__typename": "Issue"
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
