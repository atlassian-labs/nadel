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
    graphql.nadel.tests.next.update<HydrationDeferInlineFragmentGroupingTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class HydrationDeferInlineFragmentGroupingTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "monolith",
                query = """
                | query (${'$'}v0: ID!) {
                |   node(id: ${'$'}v0) {
                |     ... on Issue @defer {
                |       key
                |     }
                |     ... on Issue {
                |       __typename__hydration__assignee: __typename
                |       hydration__assignee__assigneeId: assigneeId
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "issue/1"
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "node": {
                |       "hydration__assignee__assigneeId": "user/1",
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
                    |         "node"
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
                |   "v0": "user/1"
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
     *     "node": {
     *       "assignee": {
     *         "name": "Tester"
     *       },
     *       "key": "TEST-1"
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "node": {}
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
                |         "node"
                |       ],
                |       "data": {
                |         "assignee": {
                |           "name": "Tester"
                |         },
                |         "key": "TEST-1"
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
            ),
        )
}
