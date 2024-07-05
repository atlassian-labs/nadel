// @formatter:off
package graphql.nadel.tests.next.fixtures.hydration.defer

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

public suspend fun main() {
    graphql.nadel.tests.next.update<HydrationDeferIsDisabledInListOfRelatedIssuesForParentIssueTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class HydrationDeferIsDisabledInListOfRelatedIssuesForParentIssueTestSnapshot :
        TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "issues",
                query = """
                | {
                |   issueByKey(key: "GQLGW-3") {
                |     key
                |     related {
                |       parent {
                |         hydration__assignee__assigneeId: assigneeId
                |         __typename__hydration__assignee: __typename
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issueByKey": {
                |       "key": "GQLGW-3",
                |       "related": [
                |         {
                |           "parent": null
                |         },
                |         {
                |           "parent": {
                |             "hydration__assignee__assigneeId": "ari:cloud:identity::user/1",
                |             "__typename__hydration__assignee": "Issue"
                |           }
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
                service = "users",
                query = """
                | {
                |   userById(id: "ari:cloud:identity::user/1") {
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "userById": {
                |       "name": "Franklin"
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
     *     "issueByKey": {
     *       "key": "GQLGW-3",
     *       "related": [
     *         {
     *           "parent": null
     *         },
     *         {
     *           "parent": {
     *             "assignee": {
     *               "name": "Franklin"
     *             }
     *           }
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
            |     "issueByKey": {
            |       "key": "GQLGW-3",
            |       "related": [
            |         {
            |           "parent": null
            |         },
            |         {
            |           "parent": {
            |             "assignee": {
            |               "name": "Franklin"
            |             }
            |           }
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
