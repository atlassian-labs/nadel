// @formatter:off
package graphql.nadel.tests.next.fixtures.basic

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

public suspend fun main() {
    graphql.nadel.tests.next.update<BasicObjectSchemaTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class BasicObjectSchemaTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "test",
                query = """
                | {
                |   issueById(id: "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1") {
                |     id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issueById": {
                |       "id": "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1"
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
     *     "issueById": {
     *       "id": "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1"
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
            |       "id": "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1"
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
