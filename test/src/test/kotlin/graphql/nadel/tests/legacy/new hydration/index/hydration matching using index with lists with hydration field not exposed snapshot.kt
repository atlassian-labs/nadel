// @formatter:off
package graphql.nadel.tests.legacy.`new hydration`.index

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`hydration matching using index with lists with hydration field not exposed`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `hydration matching using index with lists with hydration field not exposed snapshot` :
        TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Issues",
                query = """
                | query {
                |   issues {
                |     __typename__batch_hydration__authors: __typename
                |     id
                |     batch_hydration__authors__id: id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issues": [
                |       {
                |         "__typename__batch_hydration__authors": "Issue",
                |         "id": "ISSUE-1",
                |         "batch_hydration__authors__id": "ISSUE-1"
                |       },
                |       {
                |         "__typename__batch_hydration__authors": "Issue",
                |         "id": "ISSUE-2",
                |         "batch_hydration__authors__id": "ISSUE-2"
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
                |   usersByIssueIds(issueIds: ["ISSUE-1", "ISSUE-2"]) {
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "usersByIssueIds": [
                |       [
                |         {
                |           "name": "Name"
                |         }
                |       ],
                |       [
                |         {
                |           "name": "Name"
                |         },
                |         {
                |           "name": "Name 2"
                |         }
                |       ]
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
     *         "id": "ISSUE-1",
     *         "authors": [
     *           {
     *             "name": "Name"
     *           }
     *         ]
     *       },
     *       {
     *         "id": "ISSUE-2",
     *         "authors": [
     *           {
     *             "name": "Name"
     *           },
     *           {
     *             "name": "Name 2"
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
            |             "name": "Name"
            |           }
            |         ]
            |       },
            |       {
            |         "id": "ISSUE-2",
            |         "authors": [
            |           {
            |             "name": "Name"
            |           },
            |           {
            |             "name": "Name 2"
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
