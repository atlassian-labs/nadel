// @formatter:off
package graphql.nadel.tests.next.fixtures.stub

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<StubNestedTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class StubNestedTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "myService",
                query = """
                | {
                |   issue {
                |     __typename__stubbed__key: __typename
                |     description
                |     id
                |     related {
                |       __typename__stubbed__key: __typename
                |       __typename__stubbed__user: __typename
                |       related {
                |         __typename__stubbed__key: __typename
                |         __typename__stubbed__id: __typename
                |         related {
                |           __typename__stubbed__user: __typename
                |         }
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issue": {
                |       "id": "123",
                |       "__typename__stubbed__key": "Issue",
                |       "description": "Stop procrastinating and do the work",
                |       "related": [
                |         {
                |           "__typename__stubbed__key": "Issue",
                |           "__typename__stubbed__user": "Issue",
                |           "related": [
                |             {
                |               "__typename__stubbed__key": "Issue",
                |               "__typename__stubbed__id": "Issue",
                |               "related": []
                |             }
                |           ]
                |         },
                |         {
                |           "__typename__stubbed__key": "Issue",
                |           "__typename__stubbed__user": "Issue",
                |           "related": [
                |             {
                |               "__typename__stubbed__key": "Issue",
                |               "__typename__stubbed__id": "Issue",
                |               "related": [
                |                 {
                |                   "__typename__stubbed__user": "Issue"
                |                 }
                |               ]
                |             }
                |           ]
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
     *     "issue": {
     *       "id": "123",
     *       "description": "Stop procrastinating and do the work",
     *       "related": [
     *         {
     *           "related": [
     *             {
     *               "related": [],
     *               "id": null,
     *               "key": null
     *             }
     *           ],
     *           "user": null,
     *           "key": null
     *         },
     *         {
     *           "related": [
     *             {
     *               "related": [
     *                 {
     *                   "user": null
     *                 }
     *               ],
     *               "id": null,
     *               "key": null
     *             }
     *           ],
     *           "user": null,
     *           "key": null
     *         }
     *       ],
     *       "key": null
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "issue": {
            |       "id": "123",
            |       "description": "Stop procrastinating and do the work",
            |       "related": [
            |         {
            |           "related": [
            |             {
            |               "related": [],
            |               "id": null,
            |               "key": null
            |             }
            |           ],
            |           "user": null,
            |           "key": null
            |         },
            |         {
            |           "related": [
            |             {
            |               "related": [
            |                 {
            |                   "user": null
            |                 }
            |               ],
            |               "id": null,
            |               "key": null
            |             }
            |           ],
            |           "user": null,
            |           "key": null
            |         }
            |       ],
            |       "key": null
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
