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
public class HydrationDeferInRenamedFieldAndNestedHydrationsTestData : TestData() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "issues",
                query = """
                | {
                |   rename__issueById__getIssueById: getIssueById(id: "1") {
                |     hydration__assigneeV2__assigneeId: assigneeId
                |     __typename__hydration__assigneeV2: __typename
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                response = """
                | {
                |   "rename__issueById__getIssueById": {
                |     "hydration__assigneeV2__assigneeId": "ari:cloud:identity::user/1",
                |     "__typename__hydration__assigneeV2": "Issue"
                |   }
                | }
                """.trimMargin(),
                delayedResponses = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "issues",
                query = """
                | {
                |   rename__issueById__getIssueById: getIssueById(id: "1") {
                |     hydration__self__id: id
                |     __typename__hydration__self: __typename
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                response = """
                | {
                |   "rename__issueById__getIssueById": {
                |     "hydration__self__id": "1",
                |     "__typename__hydration__self": "Issue"
                |   }
                | }
                """.trimMargin(),
                delayedResponses = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "issues",
                query = """
                | {
                |   rename__issueById__getIssueById: getIssueById(id: "1") {
                |     hydration__self__id: id
                |     __typename__hydration__self: __typename
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                response = """
                | {
                |   "rename__issueById__getIssueById": {
                |     "hydration__self__id": "1",
                |     "__typename__hydration__self": "Issue"
                |   }
                | }
                """.trimMargin(),
                delayedResponses = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "issues",
                query = """
                | {
                |   rename__issueByKey__getIssueByKey: getIssueByKey(key: "GQLGW-1") {
                |     key
                |     hydration__self__id: id
                |     __typename__hydration__self: __typename
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                response = """
                | {
                |   "rename__issueByKey__getIssueByKey": {
                |     "key": "GQLGW-1",
                |     "hydration__self__id": "1",
                |     "__typename__hydration__self": "Issue"
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
                |   rename__quickUser__user_fast: user_fast(id: "ari:cloud:identity::user/1") {
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                response = """
                | {
                |   "rename__quickUser__user_fast": {
                |     "name": "SPEED"
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
     *       "key": "GQLGW-1",
     *       "self": {
     *         "self": {
     *           "self": {
     *             "assigneeV2": {
     *               "name": "SPEED"
     *             }
     *           }
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
            |       "key": "GQLGW-1",
            |       "self": {
            |         "self": {
            |           "self": {
            |             "assigneeV2": {
            |               "name": "SPEED"
            |             }
            |           }
            |         }
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResponses = listOfJsonStrings(
            ),
        )
}
