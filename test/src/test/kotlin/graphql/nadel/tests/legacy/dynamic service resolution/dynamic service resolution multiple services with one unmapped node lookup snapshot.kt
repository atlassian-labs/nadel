// @formatter:off
package graphql.nadel.tests.legacy.`dynamic service resolution`

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`dynamic service resolution multiple services with one unmapped node lookup`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `dynamic service resolution multiple services with one unmapped node lookup snapshot` :
        TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "IssueService",
                query = """
                | query {
                |   issue: node(id: "issue/id-123") {
                |     id
                |     ... on Issue {
                |       issueKey
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issue": {
                |       "id": "issue/id-123",
                |       "issueKey": "ISSUE-1"
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
     *     "commit": null,
     *     "issue": {
     *       "id": "issue/id-123",
     *       "issueKey": "ISSUE-1"
     *     }
     *   },
     *   "errors": [
     *     {
     *       "message": "Could not resolve service for field: /commit",
     *       "locations": [],
     *       "path": [
     *         "commit"
     *       ],
     *       "extensions": {
     *         "classification": "ExecutionAborted"
     *       }
     *     }
     *   ]
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "commit": null,
            |     "issue": {
            |       "id": "issue/id-123",
            |       "issueKey": "ISSUE-1"
            |     }
            |   },
            |   "errors": [
            |     {
            |       "message": "Could not resolve service for field: /commit",
            |       "locations": [],
            |       "path": [
            |         "commit"
            |       ],
            |       "extensions": {
            |         "classification": "ExecutionAborted"
            |       }
            |     }
            |   ]
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
