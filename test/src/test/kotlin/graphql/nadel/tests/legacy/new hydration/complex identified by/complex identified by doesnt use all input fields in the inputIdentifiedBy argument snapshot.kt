// @formatter:off
package graphql.nadel.tests.legacy.`new hydration`.`complex identified by`

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`complex identified by doesnt use all input fields in the inputIdentifiedBy argument`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class
        `complex identified by doesnt use all input fields in the inputIdentifiedBy argument snapshot`
        : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Activity",
                query = """
                | {
                |   activities {
                |     __typename__batch_hydration__issue: __typename
                |     batch_hydration__issue__context: context {
                |       issueHydrationInput {
                |         id
                |         site
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
                |     "activities": [
                |       {
                |         "id": "ACTIVITY-0",
                |         "batch_hydration__issue__context": {
                |           "issueHydrationInput": {
                |             "id": "ISSUE-0",
                |             "site": "CLOUD-0"
                |           }
                |         },
                |         "__typename__batch_hydration__issue": "Activity"
                |       },
                |       {
                |         "id": "ACTIVITY-1",
                |         "batch_hydration__issue__context": {
                |           "issueHydrationInput": {
                |             "id": "ISSUE-1",
                |             "site": "CLOUD-0"
                |           }
                |         },
                |         "__typename__batch_hydration__issue": "Activity"
                |       },
                |       {
                |         "id": "ACTIVITY-2",
                |         "batch_hydration__issue__context": {
                |           "issueHydrationInput": {
                |             "id": "ISSUE-2",
                |             "site": "CLOUD-0"
                |           }
                |         },
                |         "__typename__batch_hydration__issue": "Activity"
                |       },
                |       {
                |         "id": "ACTIVITY-3",
                |         "batch_hydration__issue__context": {
                |           "issueHydrationInput": {
                |             "id": "ISSUE-3",
                |             "site": "CLOUD-0"
                |           }
                |         },
                |         "__typename__batch_hydration__issue": "Activity"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Issue",
                query = """
                | {
                |   issues(issuesInput: [{id : "ISSUE-0", site : "CLOUD-0"}, {id : "ISSUE-1", site : "CLOUD-0"}, {id : "ISSUE-2", site : "CLOUD-0"}, {id : "ISSUE-3", site : "CLOUD-0"}]) {
                |     description
                |     issueId
                |     batch_hydration__issue__issueId: issueId
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issues": [
                |       {
                |         "issueId": "ISSUE-0",
                |         "description": "fix A",
                |         "batch_hydration__issue__issueId": "ISSUE-0"
                |       },
                |       {
                |         "issueId": "ISSUE-1",
                |         "description": "fix B",
                |         "batch_hydration__issue__issueId": "ISSUE-1"
                |       },
                |       {
                |         "issueId": "ISSUE-2",
                |         "description": "fix C",
                |         "batch_hydration__issue__issueId": "ISSUE-2"
                |       },
                |       {
                |         "issueId": "ISSUE-3",
                |         "description": "fix D",
                |         "batch_hydration__issue__issueId": "ISSUE-3"
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
     *     "activities": [
     *       {
     *         "id": "ACTIVITY-0",
     *         "issue": {
     *           "issueId": "ISSUE-0",
     *           "description": "fix A"
     *         }
     *       },
     *       {
     *         "id": "ACTIVITY-1",
     *         "issue": {
     *           "issueId": "ISSUE-1",
     *           "description": "fix B"
     *         }
     *       },
     *       {
     *         "id": "ACTIVITY-2",
     *         "issue": {
     *           "issueId": "ISSUE-2",
     *           "description": "fix C"
     *         }
     *       },
     *       {
     *         "id": "ACTIVITY-3",
     *         "issue": {
     *           "issueId": "ISSUE-3",
     *           "description": "fix D"
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
            |     "activities": [
            |       {
            |         "id": "ACTIVITY-0",
            |         "issue": {
            |           "issueId": "ISSUE-0",
            |           "description": "fix A"
            |         }
            |       },
            |       {
            |         "id": "ACTIVITY-1",
            |         "issue": {
            |           "issueId": "ISSUE-1",
            |           "description": "fix B"
            |         }
            |       },
            |       {
            |         "id": "ACTIVITY-2",
            |         "issue": {
            |           "issueId": "ISSUE-2",
            |           "description": "fix C"
            |         }
            |       },
            |       {
            |         "id": "ACTIVITY-3",
            |         "issue": {
            |           "issueId": "ISSUE-3",
            |           "description": "fix D"
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
