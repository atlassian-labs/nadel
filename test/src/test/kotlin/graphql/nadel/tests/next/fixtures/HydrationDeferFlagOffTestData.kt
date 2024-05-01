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
public class HydrationDeferFlagOffTestData : TestData() {
    override val calls: List<ExpectedServiceCall> = listOf(
                ExpectedServiceCall(
                    service = "issues",
                    query = """
                    | {
                    |   issue(id: "ari:cloud:jira::issue/1") {
                    |     id
                    |     hydration__assignee__assigneeId: assigneeId
                    |     __typename__hydration__assignee: __typename
                    |   }
                    | }
                    """.trimMargin(),
                    variables = "{}",
                    response = """
                    | {
                    |   "issue": {
                    |     "id": "ari:cloud:jira::issue/1",
                    |     "hydration__assignee__assigneeId": "ari:cloud:jira::user/1",
                    |     "__typename__hydration__assignee": "Issue"
                    |   }
                    | }
                    """.trimMargin(),
                    delayedResponses = listOfJsonStrings(
                    ),
                ),
                ExpectedServiceCall(
                    service = "users",
                    query = """
                    | {
                    |   user(id: "ari:cloud:jira::user/1") {
                    |     name
                    |   }
                    | }
                    """.trimMargin(),
                    variables = "{}",
                    response = """
                    | {
                    |   "user": {
                    |     "name": "Franklin"
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
            |     "issue": {
            |       "id": "ari:cloud:jira::issue/1",
            |       "assignee": {
            |         "name": "Franklin"
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
