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
public class HydrationTestData : TestData() {
    override val calls: List<ExpectedServiceCall> = listOf(
                ExpectedServiceCall(
                    query = """
                    | {
                    |   issueById(id: "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1") {
                    |     id
                    |     key
                    |     hydration__assignee__assigneeId: assigneeId
                    |     __typename__hydration__assignee: __typename
                    |   }
                    | }
                    """.trimMargin(),
                    variables = "{}",
                    response = """
                    | {
                    |   "issueById": {
                    |     "id": "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1",
                    |     "key": "GQLGW-1",
                    |     "hydration__assignee__assigneeId": "ari:cloud:identity::user/1",
                    |     "__typename__hydration__assignee": "Issue"
                    |   }
                    | }
                    """.trimMargin(),
                    delayedResponses = listOfJsonStrings(
                    ),
                ),
                ExpectedServiceCall(
                    query = """
                    | {
                    |   userById(id: "ari:cloud:identity::user/1") {
                    |     id
                    |     name
                    |   }
                    | }
                    """.trimMargin(),
                    variables = "{}",
                    response = """
                    | {
                    |   "userById": {
                    |     "id": "ari:cloud:identity::user/1",
                    |     "name": "Franklin Wang"
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
            |       "id": "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1",
            |       "key": "GQLGW-1",
            |       "assignee": {
            |         "id": "ari:cloud:identity::user/1",
            |         "name": "Franklin Wang"
            |       }
            |     }
            |   },
            |   "hasNext": true
            | }
            """.trimMargin(),
            delayedResponses = listOfJsonStrings(
            ),
        )
}
