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
    graphql.nadel.tests.next.update<`deep rename inside list`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `deep rename inside list snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Issues",
                query = """
                | {
                |   rename__issues__all: all {
                |     __typename__deep_rename__name: __typename
                |     deep_rename__name__details: details {
                |       key
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "rename__issues__all": [
                |       {
                |         "deep_rename__name__details": {
                |           "key": "GQLGW-1012"
                |         },
                |         "__typename__deep_rename__name": "Issue"
                |       },
                |       {
                |         "deep_rename__name__details": null,
                |         "__typename__deep_rename__name": "Issue"
                |       },
                |       {
                |         "deep_rename__name__details": {
                |           "key": "Fix the bug"
                |         },
                |         "__typename__deep_rename__name": "Issue"
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
     *     "issues": [
     *       {
     *         "name": "GQLGW-1012"
     *       },
     *       {
     *         "name": null
     *       },
     *       {
     *         "name": "Fix the bug"
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
            |     "issues": [
            |       {
            |         "name": "GQLGW-1012"
            |       },
            |       {
            |         "name": null
            |       },
            |       {
            |         "name": "Fix the bug"
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
