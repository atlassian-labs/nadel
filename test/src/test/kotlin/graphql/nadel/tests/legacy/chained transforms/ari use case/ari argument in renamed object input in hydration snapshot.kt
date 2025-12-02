// @formatter:off
package graphql.nadel.tests.legacy.`chained transforms`.`ari use case`

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`ari argument in renamed object input in hydration`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class `ari argument in renamed object input in hydration snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "MyService",
                query = """
                | query (${'$'}v0: ID!) {
                |   issue(id: ${'$'}v0) {
                |     __typename__batch_hydration__related: __typename
                |     batch_hydration__related__relatedIds: relatedIds {
                |       issueId
                |       projectId
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "ari:cloud:jira-software::issue/123"
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "issue": {
                |       "batch_hydration__related__relatedIds": [
                |         {
                |           "projectId": "ari:cloud:jira-software::project/100",
                |           "issueId": "ari:cloud:jira-software::issue/1"
                |         },
                |         {
                |           "projectId": "ari:cloud:jira-software::project/100",
                |           "issueId": "ari:cloud:jira-software::issue/2"
                |         },
                |         {
                |           "projectId": "ari:cloud:jira-software::project/101",
                |           "issueId": "ari:cloud:jira-software::issue/3"
                |         }
                |       ],
                |       "__typename__batch_hydration__related": "Issue"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "MyService",
                query = """
                | query (${'$'}v0: [UnderlyingIssueInput]) {
                |   issues(input: ${'$'}v0) {
                |     batch_hydration__related__id: id
                |     key
                |     projectId
                |     batch_hydration__related__projectId: projectId
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": [
                |     {
                |       "projectId": "100",
                |       "issueId": "1"
                |     },
                |     {
                |       "projectId": "100",
                |       "issueId": "2"
                |     },
                |     {
                |       "projectId": "101",
                |       "issueId": "3"
                |     }
                |   ]
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "issues": [
                |       {
                |         "projectId": "100",
                |         "key": "GQLGW-001",
                |         "batch_hydration__related__projectId": "100",
                |         "batch_hydration__related__id": "1"
                |       },
                |       {
                |         "projectId": "101",
                |         "key": "BUILD-003",
                |         "batch_hydration__related__projectId": "101",
                |         "batch_hydration__related__id": "3"
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
     *     "issue": {
     *       "related": [
     *         {
     *           "projectId": "ari:cloud:jira-software::project/100",
     *           "key": "GQLGW-001"
     *         },
     *         null,
     *         {
     *           "projectId": "ari:cloud:jira-software::project/101",
     *           "key": "BUILD-003"
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
            |     "issue": {
            |       "related": [
            |         {
            |           "projectId": "ari:cloud:jira-software::project/100",
            |           "key": "GQLGW-001"
            |         },
            |         null,
            |         {
            |           "projectId": "ari:cloud:jira-software::project/101",
            |           "key": "BUILD-003"
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
