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
    graphql.nadel.tests.next.update<`hydration call with fragments in the hydrated part and synthetic field`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `hydration call with fragments in the hydrated part and synthetic field snapshot` :
        TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Issues",
                query = """
                | query {
                |   issues {
                |     __typename__batch_hydration__authors: __typename
                |     batch_hydration__authors__authorDetails: authorDetails {
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
                |         "batch_hydration__authors__authorDetails": [
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
                |   userQuery {
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
                |     "userQuery": {
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
            ExpectedServiceCall(
                service = "UserService",
                query = """
                | query {
                |   userQuery {
                |     usersByIds(id: ["USER-1"]) {
                |       id
                |       name
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "userQuery": {
                |       "usersByIds": [
                |         {
                |           "name": "User 1",
                |           "id": "USER-1"
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
     *             "id": "USER-1",
     *             "name": "User 1"
     *           },
     *           {
     *             "id": "USER-2",
     *             "name": "User 2"
     *           }
     *         ]
     *       }
     *     ],
     *     "userQuery": {
     *       "usersByIds": [
     *         {
     *           "id": "USER-1",
     *           "name": "User 1"
     *         }
     *       ]
     *     }
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
            |             "id": "USER-1",
            |             "name": "User 1"
            |           },
            |           {
            |             "id": "USER-2",
            |             "name": "User 2"
            |           }
            |         ]
            |       }
            |     ],
            |     "userQuery": {
            |       "usersByIds": [
            |         {
            |           "id": "USER-1",
            |           "name": "User 1"
            |         }
            |       ]
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
