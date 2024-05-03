// @formatter:off
package graphql.nadel.tests.next.fixtures.hydration.defer

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
public class HydrationDeferInListIsDisabledForRelatedIssuesTestData : TestData() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "issues",
                query = """
                | {
                |   issueByKey(key: "GQLGW-2") {
                |     key
                |     hydration__assignee__assigneeId: assigneeId
                |     __typename__hydration__assignee: __typename
                |     related {
                |       hydration__assignee__assigneeId: assigneeId
                |       __typename__hydration__assignee: __typename
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                response = """
                | {
                |   "issueByKey": {
                |     "key": "GQLGW-2",
                |     "hydration__assignee__assigneeId": "ari:cloud:identity::user/2",
                |     "__typename__hydration__assignee": "Issue",
                |     "related": [
                |       {
                |         "hydration__assignee__assigneeId": "ari:cloud:identity::user/1",
                |         "__typename__hydration__assignee": "Issue"
                |       }
                |     ]
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
                |   userById(id: "ari:cloud:identity::user/1") {
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                response = """
                | {
                |   "userById": {
                |     "name": "Franklin"
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
                |   userById(id: "ari:cloud:identity::user/2") {
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                response = """
                | {
                |   "userById": {
                |     "name": "Tom"
                |   }
                | }
                """.trimMargin(),
                delayedResponses = listOfJsonStrings(
                ),
            ),
        )

    /**
     * ```json
     * {
     *   "data": {
     *     "issueByKey": {
     *       "key": "GQLGW-2",
     *       "related": [
     *         {
     *           "assignee": {
     *             "name": "Franklin"
     *           }
     *         }
     *       ],
     *       "assignee": {
     *         "value": {
     *           "name": "Tom"
     *         }
     *       }
     *     }
     *   }
     * }
     * ```
     */
    override val response: ExpectedNadelResponse = ExpectedNadelResponse(
            response = """
            | {
            |   "data": {
            |     "issueByKey": {
            |       "key": "GQLGW-2",
            |       "related": [
            |         {
            |           "assignee": {
            |             "name": "Franklin"
            |           }
            |         }
            |       ]
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
                |         "issueByKey",
                |         "assignee"
                |       ],
                |       "data": {
                |         "value": {
                |           "name": "Tom"
                |         }
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
            ),
        )
}
