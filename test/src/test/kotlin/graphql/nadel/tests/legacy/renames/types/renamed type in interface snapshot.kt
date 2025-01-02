// @formatter:off
package graphql.nadel.tests.legacy.renames.types

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`renamed type in interface`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `renamed type in interface snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "IssueService",
                query = """
                | query {
                |   rename__nodes__all: all {
                |     __typename
                |     id
                |     ... on Issue {
                |       links {
                |         __typename
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "rename__nodes__all": [
                |       {
                |         "__typename": "Issue",
                |         "links": null
                |       },
                |       null,
                |       {
                |         "__typename": "Issue",
                |         "links": []
                |       },
                |       {
                |         "__typename": "Issue",
                |         "links": [
                |           {
                |             "__typename": "User"
                |           },
                |           {
                |             "__typename": "Issue"
                |           },
                |           {
                |             "__typename": "Monkey"
                |           }
                |         ]
                |       },
                |       {
                |         "__typename": "Monkey"
                |       },
                |       {
                |         "__typename": "User"
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
     *         "links": null
     *       },
     *       null,
     *       {
     *         "__typename": "JiraIssue",
     *         "links": []
     *       },
     *       {
     *         "__typename": "JiraIssue",
     *         "links": [
     *           {
     *             "__typename": "User"
     *           },
     *           {
     *             "__typename": "JiraIssue"
     *           },
     *           {
     *             "__typename": "Donkey"
     *           }
     *         ]
     *       },
     *       {
     *         "__typename": "Donkey"
     *       },
     *       {
     *         "__typename": "User"
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
            |         "links": null
            |       },
            |       null,
            |       {
            |         "__typename": "JiraIssue",
            |         "links": []
            |       },
            |       {
            |         "__typename": "JiraIssue",
            |         "links": [
            |           {
            |             "__typename": "User"
            |           },
            |           {
            |             "__typename": "JiraIssue"
            |           },
            |           {
            |             "__typename": "Donkey"
            |           }
            |         ]
            |       },
            |       {
            |         "__typename": "Donkey"
            |       },
            |       {
            |         "__typename": "User"
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
