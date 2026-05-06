// @formatter:off
package graphql.nadel.tests.next.fixtures.hydration

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<ScalarHydrationTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class ScalarHydrationTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "issues",
                query = """
                | {
                |   issueById(id: "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1") {
                |     __typename__hydration__linkedCount: __typename
                |     id
                |     hydration__linkedCount__id: id
                |     key
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issueById": {
                |       "id": "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1",
                |       "key": "GQLGW-1",
                |       "hydration__linkedCount__id": "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1",
                |       "__typename__hydration__linkedCount": "Issue"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "issues",
                query = """
                | {
                |   linkedCount(id: "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1")
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "linkedCount": 100
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
     *     "issueById": {
     *       "id": "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1",
     *       "key": "GQLGW-1",
     *       "linkedCount": 100
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "issueById": {
            |       "id": "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1",
            |       "key": "GQLGW-1",
            |       "linkedCount": 100
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
