// @formatter:off
package graphql.nadel.tests.next.fixtures.hydration.defer

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<HydrationDeferGroupingTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class HydrationDeferGroupingTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "monolith",
                query = """
                | query (${'$'}v0: ID!) {
                |   issue(id: ${'$'}v0) {
                |     __typename__hydration__assignee: __typename
                |     hydration__assignee__assigneeId: assigneeId
                |     ... @defer {
                |       key
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": 1
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "issue": {
                |       "hydration__assignee__assigneeId": "1",
                |       "__typename__hydration__assignee": "Issue"
                |     }
                |   },
                |   "hasNext": true
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                    """
                    | {
                    |   "hasNext": false,
                    |   "incremental": [
                    |     {
                    |       "path": [
                    |         "issue"
                    |       ],
                    |       "data": {
                    |         "key": "TEST-1"
                    |       }
                    |     }
                    |   ]
                    | }
                    """.trimMargin(),
                ),
            ),
            ExpectedServiceCall(
                service = "monolith",
                query = """
                | query (${'$'}v0: ID!) {
                |   user(id: ${'$'}v0) {
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "1"
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "user": {
                |       "name": "Tester"
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
     *       "key": "TEST-1",
     *       "assignee": {
     *         "name": "Tester"
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
            |     "issue": {}
            |   },
            |   "hasNext": true
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
                """
                | {
                |   "hasNext": false,
                |   "incremental": [
                |     {
                |       "path": [
                |         "issue"
                |       ],
                |       "data": {
                |         "key": "TEST-1",
                |         "assignee": {
                |           "name": "Tester"
                |         }
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
            ),
        )
}
