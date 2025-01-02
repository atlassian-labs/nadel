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
    graphql.nadel.tests.next.update<`renamed type in union`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `renamed type in union snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "IssueService",
                query = """
                | {
                |   rename__nodes__all: all {
                |     __typename
                |     ... on Issue {
                |       id
                |       links {
                |         __typename
                |       }
                |     }
                |     ... on Monkey {
                |       id
                |     }
                |     ... on User {
                |       id
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
                |         "id": "1",
                |         "links": null
                |       },
                |       null,
                |       {
                |         "__typename": "Issue",
                |         "id": "2",
                |         "links": []
                |       },
                |       {
                |         "__typename": "Issue",
                |         "id": "3",
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
                |         "__typename": "Monkey",
                |         "id": "4"
                |       },
                |       {
                |         "__typename": "User",
                |         "id": "8"
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
     *         "id": "1",
     *         "links": null
     *       },
     *       null,
     *       {
     *         "__typename": "JiraIssue",
     *         "id": "2",
     *         "links": []
     *       },
     *       {
     *         "__typename": "JiraIssue",
     *         "id": "3",
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
     *         "__typename": "Donkey",
     *         "id": "4"
     *       },
     *       {
     *         "__typename": "User",
     *         "id": "8"
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
            |         "id": "1",
            |         "links": null
            |       },
            |       null,
            |       {
            |         "__typename": "JiraIssue",
            |         "id": "2",
            |         "links": []
            |       },
            |       {
            |         "__typename": "JiraIssue",
            |         "id": "3",
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
            |         "__typename": "Donkey",
            |         "id": "4"
            |       },
            |       {
            |         "__typename": "User",
            |         "id": "8"
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
