// @formatter:off
package graphql.nadel.tests.next.fixtures.hydration.defer.batch

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class BatchHydrationDeferTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "issues",
                query = """
                | {
                |   issues {
                |     key
                |     batch_hydration__assignee__assigneeId: assigneeId
                |     __typename__batch_hydration__assignee: __typename
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issues": [
                |       {
                |         "key": "GQLGW-1",
                |         "batch_hydration__assignee__assigneeId": "fwang",
                |         "__typename__batch_hydration__assignee": "Issue"
                |       },
                |       {
                |         "key": "GQLGW-2",
                |         "batch_hydration__assignee__assigneeId": "sbarker2",
                |         "__typename__batch_hydration__assignee": "Issue"
                |       },
                |       {
                |         "key": "GQLGW-3",
                |         "batch_hydration__assignee__assigneeId": "freis",
                |         "__typename__batch_hydration__assignee": "Issue"
                |       }
                |     ]
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
                |   usersByIds(ids: ["fwang", "sbarker2", "freis"]) {
                |     name
                |     batch_hydration__assignee__id: id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "usersByIds": [
                |       {
                |         "name": "Franklin",
                |         "batch_hydration__assignee__id": "fwang"
                |       },
                |       {
                |         "name": "Steven",
                |         "batch_hydration__assignee__id": "sbarker2"
                |       },
                |       {
                |         "name": "Felipe",
                |         "batch_hydration__assignee__id": "freis"
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
     *         "key": "GQLGW-1",
     *         "assignee": {
     *           "name": "Franklin"
     *         }
     *       },
     *       {
     *         "key": "GQLGW-2",
     *         "assignee": {
     *           "name": "Steven"
     *         }
     *       },
     *       {
     *         "key": "GQLGW-3",
     *         "assignee": {
     *           "name": "Felipe"
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
            |         "key": "GQLGW-1",
            |         "assignee": {
            |           "name": "Franklin"
            |         }
            |       },
            |       {
            |         "key": "GQLGW-2",
            |         "assignee": {
            |           "name": "Steven"
            |         }
            |       },
            |       {
            |         "key": "GQLGW-3",
            |         "assignee": {
            |           "name": "Felipe"
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
