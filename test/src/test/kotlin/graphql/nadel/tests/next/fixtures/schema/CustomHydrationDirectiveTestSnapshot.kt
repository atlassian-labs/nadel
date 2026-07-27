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
                | query (${'$'}v0: String!, ${'$'}v1: Int, ${'$'}v2: String, ${'$'}v3: JSON) {
                |   graphStore_query(after: ${'$'}v2, first: ${'$'}v1, other: ${'$'}v3, query: ${'$'}v0) {
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
                |   "v0": "DROP TABLE",
                |   "v1": 10,
                |   "v2": "2012",
                |   "v3": {
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
        ExpectedServiceCall(
            service = "work",
                query = """
                | {
                |   __typename__hydration__businessReport_findRecentWorkByTeam: __typename
                | }
                """.trimMargin(),
                variables = "{}",
            result = """
                | {
                |   "data": {
                |     "__typename__hydration__businessReport_findRecentWorkByTeam": "Query"
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
