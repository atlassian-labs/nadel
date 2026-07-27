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
    graphql.nadel.tests.next.update<StaticHydrationAndPolymorphicHydrationTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class StaticHydrationAndPolymorphicHydrationTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "bitbucket",
                query = """
                | query (${'$'}v0: [ID!]!) {
                |   pullRequestsByIds(ids: ${'$'}v0) {
                |     batch_hydration__node__id: id
                |     patch
                |     title
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": [
                |     "ari:cloud:bitbucket::pull-request/2"
                |   ]
                | }
                """.trimMargin(),
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
                | query (${'$'}v0: String!) {
                |   graphStore_query(query: ${'$'}v0) {
                |     edges {
                |       __typename__batch_hydration__node: __typename
                |       cursor
                |       batch_hydration__node__nodeId: nodeId
                |       batch_hydration__node__nodeId: nodeId
                |     }
                |     pageInfo {
                |       hasNextPage
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "SELECT * FROM Work WHERE teamId = ?"
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
                | query (${'$'}v0: [ID!]!) {
                |   issuesByIds(ids: ${'$'}v0) {
                |     batch_hydration__node__id: id
                |     key
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": [
                |     "ari:cloud:jira::issue/1"
                |   ]
                | }
                """.trimMargin(),
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
