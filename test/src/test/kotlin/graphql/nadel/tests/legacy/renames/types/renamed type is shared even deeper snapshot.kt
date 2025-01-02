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
    graphql.nadel.tests.next.update<`renamed type is shared even deeper`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `renamed type is shared even deeper snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Nextgen",
                query = """
                | query {
                |   elements {
                |     __typename
                |     nodes {
                |       __typename
                |       other {
                |         __typename
                |         id
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "elements": {
                |       "__typename": "ElementConnection",
                |       "nodes": [
                |         {
                |           "__typename": "Element",
                |           "other": {
                |             "__typename": "Other",
                |             "id": "OTHER-1"
                |           }
                |         }
                |       ]
                |     }
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
     *     "elements": {
     *       "__typename": "ElementConnection",
     *       "nodes": [
     *         {
     *           "__typename": "Element",
     *           "other": {
     *             "__typename": "RenamedOther",
     *             "id": "OTHER-1"
     *           }
     *         }
     *       ]
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "elements": {
            |       "__typename": "ElementConnection",
            |       "nodes": [
            |         {
            |           "__typename": "Element",
            |           "other": {
            |             "__typename": "RenamedOther",
            |             "id": "OTHER-1"
            |           }
            |         }
            |       ]
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
