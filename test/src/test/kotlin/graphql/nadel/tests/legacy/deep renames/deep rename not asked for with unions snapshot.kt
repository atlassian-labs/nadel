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
    graphql.nadel.tests.next.update<`deep rename not asked for with unions`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `deep rename not asked for with unions snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Issues",
                query = """
                | query {
                |   names {
                |     ... on Edible {
                |       name
                |     }
                |     ... on Issue {
                |       name
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
                |         "__typename": "Edible",
                |         "name": "Spaghetti"
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
     *         "name": "Spaghetti"
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
            |         "name": "Spaghetti"
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
