// @formatter:off
package graphql.nadel.tests.next.fixtures.defer

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<ReusedFragmentWithDeferLabelTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class ReusedFragmentWithDeferLabelTestSnapshot : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * query DeferredTimeTrackingRepro(
     *   $cloudId: ID!
     *   $issueKey: String!
     * ) {
     *   jira {
     *     issueByKey(cloudId: $cloudId, key: $issueKey) {
     *       id
     *       ...Sidebar
     *       ...Details
     *     }
     *   }
     * }
     *
     * fragment Sidebar on JiraIssue {
     *   ...TimeTrackingField
     * }
     *
     * fragment Details on JiraIssue {
     *   ...TimeTrackingField
     * }
     *
     * fragment TimeTrackingField on JiraIssue {
     *   timeTrackingField {
     *     id
     *     ...TimeTrackingData @defer(label: "timeTracking")
     *   }
     * }
     *
     * fragment TimeTrackingData on JiraTimeTrackingField {
     *   originalEstimateOnChildIssues {
     *     timeInSeconds
     *   }
     *   remainingEstimateOnChildIssues {
     *     timeInSeconds
     *   }
     * }
     * ```
     *
     * Variables
     *
     * ```json
     * {
     *   "cloudId": "cloud-1",
     *   "issueKey": "TEST-1"
     * }
     * ```
     */
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "jira",
                query = """
                | query DeferredTimeTrackingRepro {
                |   jira {
                |     issueByKey(cloudId: "cloud-1", key: "TEST-1") {
                |       id
                |       timeTrackingField {
                |         id
                |         ... @defer(label: "timeTracking") {
                |           originalEstimateOnChildIssues {
                |             timeInSeconds
                |           }
                |           remainingEstimateOnChildIssues {
                |             timeInSeconds
                |           }
                |         }
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "jira": {
                |       "issueByKey": {
                |         "id": "issue-1",
                |         "timeTrackingField": {
                |           "id": "time-tracking-1"
                |         }
                |       }
                |     }
                |   },
                |   "hasNext": true
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                    """
                    | {
                    |   "hasNext": false,
                    |   "incremental": [
                    |     {
                    |       "path": [
                    |         "jira",
                    |         "issueByKey",
                    |         "timeTrackingField"
                    |       ],
                    |       "label": "timeTracking",
                    |       "data": {
                    |         "originalEstimateOnChildIssues": {
                    |           "timeInSeconds": 3600
                    |         },
                    |         "remainingEstimateOnChildIssues": {
                    |           "timeInSeconds": 1800
                    |         }
                    |       }
                    |     }
                    |   ]
                    | }
                    """.trimMargin(),
                ),
            ),
        )

    /**
     * ```json
     * {
     *   "data": {
     *     "jira": {
     *       "issueByKey": {
     *         "id": "issue-1",
     *         "timeTrackingField": {
     *           "id": "time-tracking-1",
     *           "originalEstimateOnChildIssues": {
     *             "timeInSeconds": 3600
     *           },
     *           "remainingEstimateOnChildIssues": {
     *             "timeInSeconds": 1800
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
            |     "jira": {
            |       "issueByKey": {
            |         "id": "issue-1",
            |         "timeTrackingField": {
            |           "id": "time-tracking-1"
            |         }
            |       }
            |     }
            |   },
            |   "hasNext": true
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
                """
                | {
                |   "hasNext": false,
                |   "incremental": [
                |     {
                |       "path": [
                |         "jira",
                |         "issueByKey",
                |         "timeTrackingField"
                |       ],
                |       "label": "timeTracking",
                |       "data": {
                |         "originalEstimateOnChildIssues": {
                |           "timeInSeconds": 3600
                |         },
                |         "remainingEstimateOnChildIssues": {
                |           "timeInSeconds": 1800
                |         }
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
            ),
        )
}
