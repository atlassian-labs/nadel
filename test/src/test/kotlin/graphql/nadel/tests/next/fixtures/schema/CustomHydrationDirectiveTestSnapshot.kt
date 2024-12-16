// @formatter:off
package graphql.nadel.tests.next.fixtures.schema

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<CustomHydrationDirectiveTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class CustomHydrationDirectiveTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "bitbucket",
                query = """
                | {
                |   pullRequestsByIds(ids: ["ari:cloud:bitbucket::pull-request/2"]) {
                |     title
                |     patch
                |     batch_hydration__node__id: id
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "pullRequestsByIds": [
                |       {
                |         "title": "Initial Commit",
                |         "patch": "+",
                |         "batch_hydration__node__id": "ari:cloud:bitbucket::pull-request/2"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "graph_store",
                query = """
                | query (${'$'}v0: JSON) {
                |   graphStore_query(other: ${'$'}v0, query: "DROP TABLE", after: "2012", first: 10) {
                |     edges {
                |       batch_hydration__node__nodeId: nodeId
                |       batch_hydration__node__nodeId: nodeId
                |       __typename__batch_hydration__node: __typename
                |       cursor
                |     }
                |     pageInfo {
                |       hasNextPage
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": {
                |     "teamId": "hello"
                |   }
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "graphStore_query": {
                |       "edges": [
                |         {
                |           "batch_hydration__node__nodeId": "ari:cloud:jira::issue/1",
                |           "__typename__batch_hydration__node": "GraphStoreQueryEdge",
                |           "cursor": "1"
                |         },
                |         {
                |           "batch_hydration__node__nodeId": "ari:cloud:bitbucket::pull-request/2",
                |           "__typename__batch_hydration__node": "GraphStoreQueryEdge",
                |           "cursor": "2"
                |         }
                |       ],
                |       "pageInfo": {
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
                |   issuesByIds(ids: ["ari:cloud:jira::issue/1"]) {
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
     *       "edges": [
     *         {
     *           "cursor": "1",
     *           "node": {
     *             "key": "GQLGW-1"
     *           }
     *         },
     *         {
     *           "cursor": "2",
     *           "node": {
     *             "title": "Initial Commit",
     *             "patch": "+"
     *           }
     *         }
     *       ],
     *       "pageInfo": {
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
            |     "businessReport_findRecentWorkByTeam": {
            |       "edges": [
            |         {
            |           "cursor": "1",
            |           "node": {
            |             "key": "GQLGW-1"
            |           }
            |         },
            |         {
            |           "cursor": "2",
            |           "node": {
            |             "title": "Initial Commit",
            |             "patch": "+"
            |           }
            |         }
            |       ],
            |       "pageInfo": {
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
