// @formatter:off
package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`hydration with interfaces asking typename`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `hydration with interfaces asking typename snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Issues",
                query = """
                | query {
                |   ariById(id: "ari:i-always-forget-the-format/1")
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "ariById": "Definitely an ARI"
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
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
                |       __typename__hydration__id: __typename
                |       hydration__id__id: id
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
                |         "__typename__hydration__id": "User",
                |         "hydration__id__id": "ari:i-always-forget-the-format/1"
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
     *         "id": "Definitely an ARI"
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
            |         "id": "Definitely an ARI"
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
