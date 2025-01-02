// @formatter:off
package graphql.nadel.tests.legacy.renames

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`rename with interfaces asking typename`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `rename with interfaces asking typename snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Issues",
                query = """
                | query {
                |   nodes {
                |     __typename
                |     ... on Issue {
                |       id
                |     }
                |     ... on User {
                |       __typename__rename__id: __typename
                |       rename__id__ari: ari
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "nodes": [
                |       {
                |         "__typename": "Issue",
                |         "id": "GQLGW-001"
                |       },
                |       {
                |         "__typename": "Issue",
                |         "id": "GQLGW-1102"
                |       },
                |       {
                |         "__typename": "User",
                |         "__typename__rename__id": "User",
                |         "rename__id__ari": "ari:i-always-forget-the-format/1"
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
     *     "nodes": [
     *       {
     *         "__typename": "JiraIssue",
     *         "id": "GQLGW-001"
     *       },
     *       {
     *         "__typename": "JiraIssue",
     *         "id": "GQLGW-1102"
     *       },
     *       {
     *         "__typename": "User",
     *         "id": "ari:i-always-forget-the-format/1"
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
            |     "nodes": [
            |       {
            |         "__typename": "JiraIssue",
            |         "id": "GQLGW-001"
            |       },
            |       {
            |         "__typename": "JiraIssue",
            |         "id": "GQLGW-1102"
            |       },
            |       {
            |         "__typename": "User",
            |         "id": "ari:i-always-forget-the-format/1"
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
