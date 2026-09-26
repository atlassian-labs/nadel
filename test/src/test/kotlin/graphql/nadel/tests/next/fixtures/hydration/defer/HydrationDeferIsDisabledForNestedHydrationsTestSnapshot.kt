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
    graphql.nadel.tests.next.update<HydrationDeferIsDisabledForNestedHydrationsTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class HydrationDeferIsDisabledForNestedHydrationsTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "issues",
                query = """
                | query (${'$'}v0: String!) {
                |   issueByKey(key: ${'$'}v0) {
                |     __typename__hydration__assignee: __typename
                |     hydration__assignee__assigneeId: assigneeId
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "GQLGW-3"
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "issueByKey": {
                |       "hydration__assignee__assigneeId": "ari:cloud:identity::user/1",
                |       "__typename__hydration__assignee": "Issue"
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
                |   issueByKey(key: ${'$'}v0) {
                |     __typename__hydration__self: __typename
                |     key
                |     hydration__self__key: key
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "GQLGW-3"
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "issueByKey": {
                |       "key": "GQLGW-3",
                |       "hydration__self__key": "GQLGW-3",
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
                |   userById(id: ${'$'}v0) {
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
                |     "userById": {
                |       "name": "Franklin"
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
     *       "key": "GQLGW-3",
     *       "self": {
     *         "assignee": {
     *           "name": "Franklin"
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
            |       "key": "GQLGW-3",
            |       "self": {
            |         "assignee": {
            |           "name": "Franklin"
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
