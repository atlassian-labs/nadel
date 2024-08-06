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
    graphql.nadel.tests.next.update<HydrationDeferInRenamedFieldAndNestedHydrationsTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class HydrationDeferInRenamedFieldAndNestedHydrationsTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "issues",
                query = """
                | {
                |   rename__issueById__getIssueById: getIssueById(id: "1") {
                |     hydration__assigneeV2__assigneeId: assigneeId
                |     ... @defer {
                |       __typename__hydration__assigneeV2: __typename
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "rename__issueById__getIssueById": {
                |       "hydration__assigneeV2__assigneeId": "ari:cloud:identity::user/1"
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
                    |         "rename__issueById__getIssueById"
                    |       ],
                    |       "data": {
                    |         "__typename__hydration__assigneeV2": "Issue"
                    |       }
                    |     }
                    |   ]
                    | }
                    """.trimMargin(),
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
                result = """
                | {
                |   "data": {
                |     "rename__issueById__getIssueById": {
                |       "hydration__self__id": "1",
                |       "__typename__hydration__self": "Issue"
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
                |   rename__issueById__getIssueById: getIssueById(id: "1") {
                |     hydration__self__id: id
                |     __typename__hydration__self: __typename
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "rename__issueById__getIssueById": {
                |       "hydration__self__id": "1",
                |       "__typename__hydration__self": "Issue"
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
                |   rename__issueByKey__getIssueByKey: getIssueByKey(key: "GQLGW-1") {
                |     key
                |     hydration__self__id: id
                |     __typename__hydration__self: __typename
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "rename__issueByKey__getIssueByKey": {
                |       "key": "GQLGW-1",
                |       "hydration__self__id": "1",
                |       "__typename__hydration__self": "Issue"
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
     *     "issueByKey": {
     *       "key": "GQLGW-1",
     *       "self": {
     *         "self": {
     *           "self": {}
     *         }
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
            |     "issueByKey": {
            |       "key": "GQLGW-1",
            |       "self": {
            |         "self": {
            |           "self": {}
            |         }
            |       }
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
                |         "rename__issueById__getIssueById"
                |       ],
                |       "data": {
                |         "__typename__hydration__assigneeV2": "Issue"
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
            ),
        )
}
