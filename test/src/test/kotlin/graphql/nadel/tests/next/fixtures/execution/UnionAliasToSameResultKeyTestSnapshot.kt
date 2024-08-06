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
    graphql.nadel.tests.next.update<UnionAliasToSameResultKeyTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class UnionAliasToSameResultKeyTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "abstract",
                query = """
                | {
                |   abstract {
                |     __typename
                |     ... on Issue {
                |       id: key
                |     }
                |     ... on User {
                |       id: name
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "abstract": [
                |       {
                |         "__typename": "User",
                |         "id": "Hello"
                |       },
                |       {
                |         "__typename": "Issue",
                |         "id": "HEL"
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
     * ```json
     * {
     *   "data": {
     *     "abstract": [
     *       {
     *         "__typename": "User",
     *         "id": "Hello"
     *       },
     *       {
     *         "__typename": "Issue",
     *         "id": "HEL"
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
            |     "abstract": [
            |       {
            |         "__typename": "User",
            |         "id": "Hello"
            |       },
            |       {
            |         "__typename": "Issue",
            |         "id": "HEL"
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
