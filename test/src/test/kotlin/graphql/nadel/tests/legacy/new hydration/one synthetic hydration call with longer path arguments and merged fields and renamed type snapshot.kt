// @formatter:off
package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`one synthetic hydration call with longer path arguments and merged fields and renamed type`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class
        `one synthetic hydration call with longer path arguments and merged fields and renamed type snapshot`
        : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Issues",
                query = """
                | query {
                |   issues {
                |     __typename__batch_hydration__authors: __typename
                |     batch_hydration__authors__authors: authors {
                |       authorId
                |     }
                |     id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issues": [
                |       {
                |         "batch_hydration__authors__authors": [
                |           {
                |             "authorId": "USER-1"
                |           },
                |           {
                |             "authorId": "USER-2"
                |           }
                |         ],
                |         "__typename__batch_hydration__authors": "Issue",
                |         "id": "ISSUE-1"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "UserService",
                query = """
                | query {
                |   usersQuery {
                |     usersByIds(id: ["USER-1", "USER-2"]) {
                |       id
                |       batch_hydration__authors__id: id
                |       name
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "usersQuery": {
                |       "usersByIds": [
                |         {
                |           "name": "User 1",
                |           "id": "USER-1",
                |           "batch_hydration__authors__id": "USER-1"
                |         },
                |         {
                |           "name": "User 2",
                |           "id": "USER-2",
                |           "batch_hydration__authors__id": "USER-2"
                |         }
                |       ]
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
     *     "issues": [
     *       {
     *         "id": "ISSUE-1",
     *         "authors": [
     *           {
     *             "name": "User 1",
     *             "id": "USER-1"
     *           },
     *           {
     *             "name": "User 2",
     *             "id": "USER-2"
     *           }
     *         ]
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
            |         "id": "ISSUE-1",
            |         "authors": [
            |           {
            |             "name": "User 1",
            |             "id": "USER-1"
            |           },
            |           {
            |             "name": "User 2",
            |             "id": "USER-2"
            |           }
            |         ]
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
