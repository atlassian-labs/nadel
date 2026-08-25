// @formatter:off
package graphql.nadel.tests.next.fixtures.hydration

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<HydrationMultipleNestedSourceArgumentsTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class HydrationMultipleNestedSourceArgumentsTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "identity",
                query = """
                | query (${'$'}v0: ID!, ${'$'}v1: ID!, ${'$'}v2: ID!) {
                |   issueUser(issueId: ${'$'}v1, siteId: ${'$'}v0, userId: ${'$'}v2) {
                |     id
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "ari:cloud:platform::site/123",
                |   "v1": "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1",
                |   "v2": "ari:cloud:identity::user/1"
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "issueUser": {
                |       "id": "ari:cloud:identity::user/1",
                |       "name": "Franklin Wang"
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
                |   issueById(id: ${'$'}v0) {
                |     __typename__hydration__assignee: __typename
                |     hydration__assignee__assigneeCriteria: assigneeCriteria {
                |       assigneeId
                |       siteId
                |     }
                |     id
                |     hydration__assignee__id: id
                |     key
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1"
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "issueById": {
                |       "id": "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1",
                |       "key": "GQLGW-1",
                |       "hydration__assignee__assigneeCriteria": {
                |         "siteId": "ari:cloud:platform::site/123",
                |         "assigneeId": "ari:cloud:identity::user/1"
                |       },
                |       "hydration__assignee__id": "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1",
                |       "__typename__hydration__assignee": "Issue"
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
     *       "id": "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1",
     *       "key": "GQLGW-1",
     *       "assignee": {
     *         "id": "ari:cloud:identity::user/1",
     *         "name": "Franklin Wang"
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
            |     "issueById": {
            |       "id": "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1",
            |       "key": "GQLGW-1",
            |       "assignee": {
            |         "id": "ari:cloud:identity::user/1",
            |         "name": "Franklin Wang"
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
