// @formatter:off
package graphql.nadel.tests.next.fixtures.hydration.defer

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<HydrationDeferIsDisabledTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class HydrationDeferIsDisabledTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "issues",
                query = """
                | {
                |   issues {
                |     key
                |     hydration__assignee__assigneeId: assigneeId
                |     ... @defer {
                |       __typename__hydration__assignee: __typename
                |     }
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
                |         "hydration__assignee__assigneeId": "ari:cloud:identity::user/1"
                |       },
                |       {
                |         "key": "GQLGW-2",
                |         "hydration__assignee__assigneeId": "ari:cloud:identity::user/2"
                |       },
                |       {
                |         "key": "GQLGW-3",
                |         "hydration__assignee__assigneeId": "ari:cloud:identity::user/1"
                |       }
                |     ]
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
                    |         "issues",
                    |         2
                    |       ],
                    |       "data": {
                    |         "assignee": null
                    |       }
                    |     }
                    |   ]
                    | }
                    """.trimMargin(),
                    """
                    | {
                    |   "hasNext": true,
                    |   "incremental": [
                    |     {
                    |       "path": [
                    |         "issues",
                    |         0
                    |       ],
                    |       "data": {
                    |         "assignee": null
                    |       }
                    |     }
                    |   ]
                    | }
                    """.trimMargin(),
                    """
                    | {
                    |   "hasNext": true,
                    |   "incremental": [
                    |     {
                    |       "path": [
                    |         "issues",
                    |         1
                    |       ],
                    |       "data": {
                    |         "assignee": null
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
     *     "issues": [
     *       {
     *         "key": "GQLGW-1",
     *         "assignee": null
     *       },
     *       {
     *         "key": "GQLGW-2",
     *         "assignee": null
     *       },
     *       {
     *         "key": "GQLGW-3",
     *         "assignee": null
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
            |         "key": "GQLGW-1"
            |       },
            |       {
            |         "key": "GQLGW-2"
            |       },
            |       {
            |         "key": "GQLGW-3"
            |       }
            |     ]
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
                |         "issues",
                |         2
                |       ],
                |       "data": {
                |         "assignee": null
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
                """
                | {
                |   "hasNext": true,
                |   "incremental": [
                |     {
                |       "path": [
                |         "issues",
                |         0
                |       ],
                |       "data": {
                |         "assignee": null
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
                """
                | {
                |   "hasNext": true,
                |   "incremental": [
                |     {
                |       "path": [
                |         "issues",
                |         1
                |       ],
                |       "data": {
                |         "assignee": null
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
            ),
        )
}
