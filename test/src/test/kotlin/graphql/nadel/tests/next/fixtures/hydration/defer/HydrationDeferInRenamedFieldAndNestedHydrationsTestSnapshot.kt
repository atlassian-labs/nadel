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
                | query (${'$'}v0: ID!) {
                |   rename__issueById__getIssueById: getIssueById(id: ${'$'}v0) {
                |     __typename__hydration__assigneeV2: __typename
                |     hydration__assigneeV2__assigneeId: assigneeId
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "1"
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "rename__issueById__getIssueById": {
                |       "hydration__assigneeV2__assigneeId": "ari:cloud:identity::user/1",
                |       "__typename__hydration__assigneeV2": "Issue"
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
                | query (${'$'}v0: ID!) {
                |   rename__issueById__getIssueById: getIssueById(id: ${'$'}v0) {
                |     __typename__hydration__self: __typename
                |     hydration__self__id: id
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "1"
                | }
                """.trimMargin(),
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
                | query (${'$'}v0: ID!) {
                |   rename__issueById__getIssueById: getIssueById(id: ${'$'}v0) {
                |     __typename__hydration__self: __typename
                |     hydration__self__id: id
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "1"
                | }
                """.trimMargin(),
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
                | query (${'$'}v0: String!) {
                |   rename__issueByKey__getIssueByKey: getIssueByKey(key: ${'$'}v0) {
                |     __typename__hydration__self: __typename
                |     hydration__self__id: id
                |     key
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "GQLGW-1"
                | }
                """.trimMargin(),
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
            ExpectedServiceCall(
                service = "users",
                query = """
                | query (${'$'}v0: ID!) {
                |   rename__quickUser__user_fast: user_fast(id: ${'$'}v0) {
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "ari:cloud:identity::user/1"
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "rename__quickUser__user_fast": {
                |       "name": "SPEED"
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
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
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
            delayedResults = listOfJsonStrings(
            ),
        )
}
