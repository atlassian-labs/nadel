// @formatter:off
package graphql.nadel.tests.next.fixtures.partition

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<PartitionWithConflictingRoutingValuesThatAreFilteredTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class PartitionWithConflictingRoutingValuesThatAreFilteredTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "things_service",
                query = """
                | query getPartitionedConnections {
                |   things(filter: {thingsIds : [{to : "thing-1-primary:partition-A", from : "thing-1-secondary:partition-B"}, {to : "thing-4-secondary:partition-B", from : "thing-4-primary:partition-A"}]}) {
                |     to {
                |       id
                |       name
                |     }
                |     from {
                |       id
                |       name
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "things": [
                |       {
                |         "to": {
                |           "id": "thing-1-primary",
                |           "name": "THING-1-PRIMARY"
                |         },
                |         "from": {
                |           "id": "thing-1-secondary",
                |           "name": "THING-1-SECONDARY"
                |         }
                |       },
                |       {
                |         "to": {
                |           "id": "thing-4-secondary",
                |           "name": "THING-4-SECONDARY"
                |         },
                |         "from": {
                |           "id": "thing-4-primary",
                |           "name": "THING-4-PRIMARY"
                |         }
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "things_service",
                query = """
                | query getPartitionedConnections {
                |   things(filter: {thingsIds : [{to : "thing-2-secondary:partition-A", from : "thing-2-primary:partition-B"}, {to : "thing-3-primary:partition-B", from : "thing-3-secondary:partition-A"}]}) {
                |     to {
                |       id
                |       name
                |     }
                |     from {
                |       id
                |       name
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "things": [
                |       {
                |         "to": {
                |           "id": "thing-2-secondary",
                |           "name": "THING-2-SECONDARY"
                |         },
                |         "from": {
                |           "id": "thing-2-primary",
                |           "name": "THING-2-PRIMARY"
                |         }
                |       },
                |       {
                |         "to": {
                |           "id": "thing-3-primary",
                |           "name": "THING-3-PRIMARY"
                |         },
                |         "from": {
                |           "id": "thing-3-secondary",
                |           "name": "THING-3-SECONDARY"
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
     *     "things": [
     *       {
     *         "to": {
     *           "id": "thing-1-primary",
     *           "name": "THING-1-PRIMARY"
     *         },
     *         "from": {
     *           "id": "thing-1-secondary",
     *           "name": "THING-1-SECONDARY"
     *         }
     *       },
     *       {
     *         "to": {
     *           "id": "thing-4-secondary",
     *           "name": "THING-4-SECONDARY"
     *         },
     *         "from": {
     *           "id": "thing-4-primary",
     *           "name": "THING-4-PRIMARY"
     *         }
     *       },
     *       {
     *         "to": {
     *           "id": "thing-2-secondary",
     *           "name": "THING-2-SECONDARY"
     *         },
     *         "from": {
     *           "id": "thing-2-primary",
     *           "name": "THING-2-PRIMARY"
     *         }
     *       },
     *       {
     *         "to": {
     *           "id": "thing-3-primary",
     *           "name": "THING-3-PRIMARY"
     *         },
     *         "from": {
     *           "id": "thing-3-secondary",
     *           "name": "THING-3-SECONDARY"
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
            |     "things": [
            |       {
            |         "to": {
            |           "id": "thing-1-primary",
            |           "name": "THING-1-PRIMARY"
            |         },
            |         "from": {
            |           "id": "thing-1-secondary",
            |           "name": "THING-1-SECONDARY"
            |         }
            |       },
            |       {
            |         "to": {
            |           "id": "thing-4-secondary",
            |           "name": "THING-4-SECONDARY"
            |         },
            |         "from": {
            |           "id": "thing-4-primary",
            |           "name": "THING-4-PRIMARY"
            |         }
            |       },
            |       {
            |         "to": {
            |           "id": "thing-2-secondary",
            |           "name": "THING-2-SECONDARY"
            |         },
            |         "from": {
            |           "id": "thing-2-primary",
            |           "name": "THING-2-PRIMARY"
            |         }
            |       },
            |       {
            |         "to": {
            |           "id": "thing-3-primary",
            |           "name": "THING-3-PRIMARY"
            |         },
            |         "from": {
            |           "id": "thing-3-secondary",
            |           "name": "THING-3-SECONDARY"
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
