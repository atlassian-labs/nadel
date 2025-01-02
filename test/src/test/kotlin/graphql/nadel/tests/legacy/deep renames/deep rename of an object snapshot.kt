// @formatter:off
package graphql.nadel.tests.legacy.`deep renames`

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`deep rename of an object`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `deep rename of an object snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Issues",
                query = """
                | query {
                |   issues {
                |     __typename__deep_rename__authorName: __typename
                |     deep_rename__authorName__authorDetails: authorDetails {
                |       name {
                |         __typename__rename__firstName: __typename
                |         __typename__rename__lastName: __typename
                |         rename__firstName__fName: fName
                |         rename__lastName__lName: lName
                |       }
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
                |         "__typename__deep_rename__authorName": "Issue",
                |         "id": "ISSUE-1",
                |         "deep_rename__authorName__authorDetails": {
                |           "name": {
                |             "__typename__rename__firstName": "Name",
                |             "rename__lastName__lName": "Smith",
                |             "__typename__rename__lastName": "Name",
                |             "rename__firstName__fName": "George"
                |           }
                |         }
                |       },
                |       {
                |         "__typename__deep_rename__authorName": "Issue",
                |         "id": "ISSUE-2",
                |         "deep_rename__authorName__authorDetails": {
                |           "name": {
                |             "__typename__rename__firstName": "Name",
                |             "rename__lastName__lName": "Windsor",
                |             "__typename__rename__lastName": "Name",
                |             "rename__firstName__fName": "Elizabeth"
                |           }
                |         }
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
     *         "id": "ISSUE-1",
     *         "authorName": {
     *           "firstName": "George",
     *           "lastName": "Smith"
     *         }
     *       },
     *       {
     *         "id": "ISSUE-2",
     *         "authorName": {
     *           "firstName": "Elizabeth",
     *           "lastName": "Windsor"
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
            |         "id": "ISSUE-1",
            |         "authorName": {
            |           "firstName": "George",
            |           "lastName": "Smith"
            |         }
            |       },
            |       {
            |         "id": "ISSUE-2",
            |         "authorName": {
            |           "firstName": "Elizabeth",
            |           "lastName": "Windsor"
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
