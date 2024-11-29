// @formatter:off
package graphql.nadel.tests.next.fixtures.hydration.copy

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<HydrationRemainingArgumentsTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class HydrationRemainingArgumentsTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "graph_store",
                query = """
                | query (${'$'}v0: JSON) {
                |   graphStore_query(query: "SELECT * FROM Work WHERE teamId = ?", remainingArgs: ${'$'}v0) {
                |     __typename
                |     edges {
                |       batch_hydration__node__nodeId: nodeId
                |       __typename__batch_hydration__node: __typename
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": {
                |     "orgId": "turtles",
                |     "teamId": null
                |   }
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "graphStore_query": {
                |       "__typename": "GraphStoreQueryConnection",
                |       "edges": [
                |         {
                |           "batch_hydration__node__nodeId": "ari:cloud:jira::issue/1",
                |           "__typename__batch_hydration__node": "GraphStoreQueryEdge"
                |         }
                |       ]
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "jira",
                query = """
                | {
                |   issuesByIds(ids: ["ari:cloud:jira::issue/1"]) {
                |     __typename
                |     key
                |     batch_hydration__node__id: id
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "issuesByIds": [
                |       {
                |         "__typename": "JiraIssue",
                |         "key": "GQLGW-1",
                |         "batch_hydration__node__id": "ari:cloud:jira::issue/1"
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
     *     "businessReport_findRecentWorkByTeam": {
     *       "__typename": "WorkConnection",
     *       "edges": [
     *         {
     *           "node": {
     *             "__typename": "JiraIssue",
     *             "key": "GQLGW-1"
     *           }
     *         }
     *       ]
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "businessReport_findRecentWorkByTeam": {
            |       "__typename": "WorkConnection",
            |       "edges": [
            |         {
            |           "node": {
            |             "__typename": "JiraIssue",
            |             "key": "GQLGW-1"
            |           }
            |         }
            |       ]
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
