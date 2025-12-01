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
    graphql.nadel.tests.next.update<HydrationNestedObjectInputTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class HydrationNestedObjectInputTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "identity",
                query = """
                | {
                |   userByFilter(filter: {filter : [{parentId : "123", subEntityTypes : ["a", "b", "c"]}]}) {
                |     id
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "userByFilter": {
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
                | {
                |   issues {
                |     __typename__hydration__assignee: __typename
                |     hydration__assignee__assigneeFilter: assigneeFilter {
                |       filter {
                |         parentId
                |         subEntityTypes
                |       }
                |     }
                |     id
                |     key
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issues": [
                |       {
                |         "id": "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1",
                |         "key": "GQLGW-1",
                |         "hydration__assignee__assigneeFilter": {
                |           "filter": [
                |             {
                |               "parentId": "123",
                |               "subEntityTypes": [
                |                 "a",
                |                 "b",
                |                 "c"
                |               ]
                |             }
                |           ]
                |         },
                |         "__typename__hydration__assignee": "Issue"
                |       }
                |     ]
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
     *     "issues": [
     *       {
     *         "id": "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1",
     *         "key": "GQLGW-1",
     *         "assignee": {
     *           "id": "ari:cloud:identity::user/1",
     *           "name": "Franklin Wang"
     *         }
     *       }
     *     ]
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "issues": [
            |       {
            |         "id": "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1",
            |         "key": "GQLGW-1",
            |         "assignee": {
            |           "id": "ari:cloud:identity::user/1",
            |           "name": "Franklin Wang"
            |         }
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
