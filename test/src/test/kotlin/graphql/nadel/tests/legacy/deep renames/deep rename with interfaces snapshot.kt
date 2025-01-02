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
    graphql.nadel.tests.next.update<`deep rename with interfaces`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `deep rename with interfaces snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Issues",
                query = """
                | {
                |   names {
                |     ... on Issue {
                |       name
                |     }
                |     ... on User {
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
                |         "name": "GQLGW-001"
                |       },
                |       {
                |         "name": "GQLGW-1102"
                |       },
                |       {
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
     *         "name": "GQLGW-001"
     *       },
     *       {
     *         "name": "GQLGW-1102"
     *       },
     *       {
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
            |         "name": "GQLGW-001"
            |       },
            |       {
            |         "name": "GQLGW-1102"
            |       },
            |       {
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
