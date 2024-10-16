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
    graphql.nadel.tests.next.update<NestedPartitionTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class NestedPartitionTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "things_service",
                query = """
                | query getPartitionedThings {
                |   things(filter: {thingsIds : [{id : "thing-1:partition-A"}, {id : "thing-3:partition-A"}]}) {
                |     id
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "things": [
                |       {
                |         "id": "thing-1",
                |         "name": "THING-1"
                |       },
                |       {
                |         "id": "thing-3",
                |         "name": "THING-3"
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
                | query getPartitionedThings {
                |   things(filter: {thingsIds : [{id : "thing-2:partition-B"}, {id : "thing-4:partition-B"}]}) {
                |     id
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "things": [
                |       {
                |         "id": "thing-2",
                |         "name": "THING-2"
                |       },
                |       {
                |         "id": "thing-4",
                |         "name": "THING-4"
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
     *         "id": "thing-1",
     *         "name": "THING-1"
     *       },
     *       {
     *         "id": "thing-3",
     *         "name": "THING-3"
     *       },
     *       {
     *         "id": "thing-2",
     *         "name": "THING-2"
     *       },
     *       {
     *         "id": "thing-4",
     *         "name": "THING-4"
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
            |         "id": "thing-1",
            |         "name": "THING-1"
            |       },
            |       {
            |         "id": "thing-3",
            |         "name": "THING-3"
            |       },
            |       {
            |         "id": "thing-2",
            |         "name": "THING-2"
            |       },
            |       {
            |         "id": "thing-4",
            |         "name": "THING-4"
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
