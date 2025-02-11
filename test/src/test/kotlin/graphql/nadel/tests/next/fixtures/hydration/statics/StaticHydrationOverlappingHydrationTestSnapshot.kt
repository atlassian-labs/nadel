// @formatter:off
package graphql.nadel.tests.next.fixtures.hydration.statics

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<StaticHydrationOverlappingHydrationTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class StaticHydrationOverlappingHydrationTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "graph_store",
                query = """
                | {
                |   graphStore_query(query: "Hello World") {
                |     edges {
                |       __typename__batch_hydration__node: __typename
                |       batch_hydration__node__nodeId: nodeId
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "graphStore_query": {
                |       "edges": [
                |         {
                |           "batch_hydration__node__nodeId": "ari:cloud:jira::issue/1",
                |           "__typename__batch_hydration__node": "GraphStoreQueryEdge"
                |         },
                |         {
                |           "batch_hydration__node__nodeId": "ari:cloud:jira::comment/2",
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
                service = "graph_store",
                query = """
                | {
                |   graphStore_query(query: "SELECT * FROM Work WHERE teamId = ?") {
                |     __typename
                |     edges {
                |       __typename
                |       __typename__batch_hydration__node: __typename
                |       cursor
                |       batch_hydration__node__nodeId: nodeId
                |       batch_hydration__node__nodeId: nodeId
                |     }
                |     pageInfo {
                |       __typename
                |       hasNextPage
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "graphStore_query": {
                |       "__typename": "GraphStoreQueryConnection",
                |       "edges": [
                |         {
                |           "__typename": "GraphStoreQueryEdge",
                |           "batch_hydration__node__nodeId": "ari:cloud:jira::issue/1",
                |           "__typename__batch_hydration__node": "GraphStoreQueryEdge",
                |           "cursor": "1"
                |         },
                |         {
                |           "__typename": "GraphStoreQueryEdge",
                |           "batch_hydration__node__nodeId": "ari:cloud:jira::comment/2",
                |           "__typename__batch_hydration__node": "GraphStoreQueryEdge",
                |           "cursor": "1"
                |         }
                |       ],
                |       "pageInfo": {
                |         "__typename": "PageInfo",
                |         "hasNextPage": true
                |       }
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
                |   comments(ids: ["ari:cloud:jira::comment/2"]) {
                |     __typename
                |     batch_hydration__node__id: id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "comments": [
                |       {
                |         "__typename": "JiraComment",
                |         "batch_hydration__node__id": "ari:cloud:jira::comment/2"
                |       }
                |     ]
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
                |   comments(ids: ["ari:cloud:jira::issue/1", "ari:cloud:jira::comment/2"]) {
                |     __typename
                |     batch_hydration__node__id: id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "comments": [
                |       null,
                |       {
                |         "__typename": "JiraComment",
                |         "batch_hydration__node__id": "ari:cloud:jira::comment/2"
                |       }
                |     ]
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
                |   issues(ids: ["ari:cloud:jira::issue/1"]) {
                |     __typename
                |     batch_hydration__node__id: id
                |     key
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issues": [
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
     *     "graphStore_query": {
     *       "edges": [
     *         {
     *           "node": null
     *         },
     *         {
     *           "node": {
     *             "__typename": "JiraComment"
     *           }
     *         }
     *       ]
     *     },
     *     "businessReport_findRecentWorkByTeam": {
     *       "__typename": "WorkConnection",
     *       "edges": [
     *         {
     *           "__typename": "WorkEdge",
     *           "cursor": "1",
     *           "node": {
     *             "__typename": "JiraIssue",
     *             "key": "GQLGW-1"
     *           }
     *         },
     *         {
     *           "__typename": "WorkEdge",
     *           "cursor": "1",
     *           "node": {
     *             "__typename": "JiraComment"
     *           }
     *         }
     *       ],
     *       "pageInfo": {
     *         "__typename": "PageInfo",
     *         "hasNextPage": true
     *       }
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "graphStore_query": {
            |       "edges": [
            |         {
            |           "node": null
            |         },
            |         {
            |           "node": {
            |             "__typename": "JiraComment"
            |           }
            |         }
            |       ]
            |     },
            |     "businessReport_findRecentWorkByTeam": {
            |       "__typename": "WorkConnection",
            |       "edges": [
            |         {
            |           "__typename": "WorkEdge",
            |           "cursor": "1",
            |           "node": {
            |             "__typename": "JiraIssue",
            |             "key": "GQLGW-1"
            |           }
            |         },
            |         {
            |           "__typename": "WorkEdge",
            |           "cursor": "1",
            |           "node": {
            |             "__typename": "JiraComment"
            |           }
            |         }
            |       ],
            |       "pageInfo": {
            |         "__typename": "PageInfo",
            |         "hasNextPage": true
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
