// @formatter:off
package graphql.nadel.tests.next.fixtures.stub

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings

private suspend fun main() {
    graphql.nadel.tests.next.update<StubListTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class StubListTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "myService",
                query = """
                | {
                |   issues {
                |     __typename__stubbed__key: __typename
                |     description
                |     id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issues": [
                |       {
                |         "id": "123",
                |         "__typename__stubbed__key": "Issue",
                |         "description": "Stop procrastinating and do the work"
                |       },
                |       null,
                |       {
                |         "id": "456",
                |         "__typename__stubbed__key": "Issue",
                |         "description": "Wow they're piling up"
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
     *         "id": "123",
     *         "description": "Stop procrastinating and do the work",
     *         "key": null
     *       },
     *       null,
     *       {
     *         "id": "456",
     *         "description": "Wow they're piling up",
     *         "key": null
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
            |         "id": "123",
            |         "description": "Stop procrastinating and do the work",
            |         "key": null
            |       },
            |       null,
            |       {
            |         "id": "456",
            |         "description": "Wow they're piling up",
            |         "key": null
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
