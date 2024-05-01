// @formatter:off
package graphql.nadel.tests.next.fixtures

import graphql.nadel.tests.next.ExpectedNadelResponse
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestData
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.CaptureTestData]
 */
@Suppress("unused")
public class BasicObjectSchemaTestData : TestData() {
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
                    response = """
                    | {
                    |   "issueById": {
                    |     "id": "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1"
                    |   }
                    | }
                    """.trimMargin(),
                    delayedResponses = listOfJsonStrings(
                    ),
                ),
        )

    override val response: ExpectedNadelResponse = ExpectedNadelResponse(
            response = """
            | {
            |   "data": {
            |     "issueById": {
            |       "id": "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1"
            |     }
            |   },
            |   "hasNext": true
            | }
            """.trimMargin(),
            delayedResponses = listOfJsonStrings(
            ),
        )
}
