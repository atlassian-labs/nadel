// @formatter:off
package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<BatchHydrationCoalescingConsumerMetadataTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class BatchHydrationCoalescingConsumerMetadataTestSnapshot : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * query {
     *   issues {
     *     assignee {
     *       name
     *     }
     *     reporter {
     *       name
     *     }
     *   }
     * }
     * ```
     *
     * Variables
     *
     * ```json
     * {}
     * ```
     */
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Identity",
                query = """
                | {
                |   batch_hydration__0_0: usersByIds(ids: ["ari:cloud:identity::user/1", "ari:cloud:identity::user/2", "ari:cloud:identity::user/3"]) {
                |     batch_hydration_shared_0_0__assignee__id: id
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "batch_hydration__0_0": [
                |       {
                |         "name": "One",
                |         "batch_hydration_shared_0_0__assignee__id": "ari:cloud:identity::user/1"
                |       },
                |       {
                |         "name": "Two",
                |         "batch_hydration_shared_0_0__assignee__id": "ari:cloud:identity::user/2"
                |       },
                |       {
                |         "name": "Three",
                |         "batch_hydration_shared_0_0__assignee__id": "ari:cloud:identity::user/3"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Jira",
                query = """
                | {
                |   issues {
                |     __typename__batch_hydration__assignee: __typename
                |     __typename__batch_hydration__reporter: __typename
                |     batch_hydration__assignee__assigneeId: assigneeId
                |     batch_hydration__reporter__reporterId: reporterId
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issues": [
                |       {
                |         "batch_hydration__assignee__assigneeId": "ari:cloud:identity::user/1",
                |         "__typename__batch_hydration__assignee": "Issue",
                |         "batch_hydration__reporter__reporterId": "ari:cloud:identity::user/2",
                |         "__typename__batch_hydration__reporter": "Issue"
                |       },
                |       {
                |         "batch_hydration__assignee__assigneeId": "ari:cloud:identity::user/1",
                |         "__typename__batch_hydration__assignee": "Issue",
                |         "batch_hydration__reporter__reporterId": "ari:cloud:identity::user/3",
                |         "__typename__batch_hydration__reporter": "Issue"
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
     *         "reporter": {
     *           "name": "Two"
     *         },
     *         "assignee": {
     *           "name": "One"
     *         }
     *       },
     *       {
     *         "reporter": {
     *           "name": "Three"
     *         },
     *         "assignee": {
     *           "name": "One"
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
            |         "reporter": {
            |           "name": "Two"
            |         },
            |         "assignee": {
            |           "name": "One"
            |         }
            |       },
            |       {
            |         "reporter": {
            |           "name": "Three"
            |         },
            |         "assignee": {
            |           "name": "One"
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
