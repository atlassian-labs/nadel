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
    graphql.nadel.tests.next.update<`deep rename of an object with transformations inside object`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `deep rename of an object with transformations inside object snapshot` : TestSnapshot()
        {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Issues",
                query = """
                | {
                |   issues {
                |     __typename__deep_rename__authorName: __typename
                |     deep_rename__authorName__authorDetails: authorDetails {
                |       name {
                |         __typename__rename__firstName: __typename
                |         lastName
                |         rename__firstName__originalFirstName: originalFirstName
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
                |         "id": "ISSUE-1",
                |         "deep_rename__authorName__authorDetails": {
                |           "name": {
                |             "rename__firstName__originalFirstName": "George",
                |             "__typename__rename__firstName": "OriginalName",
                |             "lastName": "Smith"
                |           }
                |         },
                |         "__typename__deep_rename__authorName": "Issue"
                |       },
                |       {
                |         "id": "ISSUE-2",
                |         "deep_rename__authorName__authorDetails": {
                |           "name": {
                |             "rename__firstName__originalFirstName": "Elizabeth",
                |             "__typename__rename__firstName": "OriginalName",
                |             "lastName": "Windsor"
                |           }
                |         },
                |         "__typename__deep_rename__authorName": "Issue"
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
     *           "lastName": "Smith",
     *           "firstName": "George"
     *         }
     *       },
     *       {
     *         "id": "ISSUE-2",
     *         "authorName": {
     *           "lastName": "Windsor",
     *           "firstName": "Elizabeth"
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
            |           "lastName": "Smith",
            |           "firstName": "George"
            |         }
            |       },
            |       {
            |         "id": "ISSUE-2",
            |         "authorName": {
            |           "lastName": "Windsor",
            |           "firstName": "Elizabeth"
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
