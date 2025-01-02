// @formatter:off
package graphql.nadel.tests.legacy.`extend type`

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`extending types via hydration returning a connection`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `extending types via hydration returning a connection snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Association",
                query = """
                | query {
                |   association(filter: {name: "value"}, id: "ISSUE-1") {
                |     nodes {
                |       __typename__hydration__page: __typename
                |       hydration__page__pageId: pageId
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "association": {
                |       "nodes": [
                |         {
                |           "__typename__hydration__page": "Association",
                |           "hydration__page__pageId": "1"
                |         }
                |       ]
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Association",
                query = """
                | query {
                |   pages {
                |     page(id: "1") {
                |       id
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "pages": {
                |       "page": {
                |         "id": "1"
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Issue",
                query = """
                | query {
                |   synth {
                |     issue {
                |       __typename__hydration__association: __typename
                |       hydration__association__id: id
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "synth": {
                |       "issue": {
                |         "hydration__association__id": "ISSUE-1",
                |         "__typename__hydration__association": "Issue"
                |       }
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
     *     "synth": {
     *       "issue": {
     *         "association": {
     *           "nodes": [
     *             {
     *               "page": {
     *                 "id": "1"
     *               }
     *             }
     *           ]
     *         }
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
            |     "synth": {
            |       "issue": {
            |         "association": {
            |           "nodes": [
            |             {
            |               "page": {
            |                 "id": "1"
            |               }
            |             }
            |           ]
            |         }
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
