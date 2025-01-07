// @formatter:off
package graphql.nadel.tests.legacy.`deep renames`

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`deep rename with interfaces asking typename`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `deep rename with interfaces asking typename snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Issues",
                query = """
                | {
                |   names {
                |     ... on Issue {
                |       __typename
                |       name
                |     }
                |     ... on User {
                |       __typename
                |       __typename__deep_rename__name: __typename
                |       deep_rename__name__details: details {
                |         firstName
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "names": [
                |       {
                |         "__typename": "Issue",
                |         "name": "GQLGW-001"
                |       },
                |       {
                |         "__typename": "Issue",
                |         "name": "GQLGW-1102"
                |       },
                |       {
                |         "__typename": "User",
                |         "deep_rename__name__details": {
                |           "firstName": "Franklin"
                |         },
                |         "__typename__deep_rename__name": "User"
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
     *     "names": [
     *       {
     *         "__typename": "JiraIssue",
     *         "name": "GQLGW-001"
     *       },
     *       {
     *         "__typename": "JiraIssue",
     *         "name": "GQLGW-1102"
     *       },
     *       {
     *         "__typename": "User",
     *         "name": "Franklin"
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
            |     "names": [
            |       {
            |         "__typename": "JiraIssue",
            |         "name": "GQLGW-001"
            |       },
            |       {
            |         "__typename": "JiraIssue",
            |         "name": "GQLGW-1102"
            |       },
            |       {
            |         "__typename": "User",
            |         "name": "Franklin"
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
