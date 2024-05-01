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
public class HydrationDeferTestData : TestData() {
    override val calls: List<ExpectedServiceCall> = listOf(
                ExpectedServiceCall(
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
                    |   "data": {
                    |     "issue": {
                    |       "id": "ari:cloud:jira::issue/1"
                    |     }
                    |   }
                    | }
                    """.trimMargin(),
                    delayedResponses = listOfJsonStrings(
                    ),
                ),
                ExpectedServiceCall(
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
                    |   "data": {
                    |     "user": {
                    |       "name": "Franklin"
                    |     }
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
            |       "id": "ari:cloud:jira::issue/1"
            |     }
            |   },
            |   "hasNext": true
            | }
            """.trimMargin(),
            delayedResponses = listOfJsonStrings(
                """
                | {
                |   "hasNext": false,
                |   "incremental": [
                |     {
                |       "path": [
                |         "issue",
                |         "assignee"
                |       ],
                |       "data": {
                |         "value": {
                |           "name": "Franklin"
                |         }
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
            ),
        )
}
