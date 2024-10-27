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
    graphql.nadel.tests.next.update<PartitionFollowedByRenamedTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class PartitionFollowedByRenamedTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "things_service",
                query = """
                | query getPartitionedThings {
                |   things(ids: ["thing-1:partition-A", "thing-3:partition-A"]) {
                |     id
                |     rename__name__underlyingName: underlyingName
                |     __typename__rename__name: __typename
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
                |         "rename__name__underlyingName": "THING-1",
                |         "__typename__rename__name": "Thing"
                |       },
                |       {
                |         "id": "thing-3",
                |         "rename__name__underlyingName": "THING-3",
                |         "__typename__rename__name": "Thing"
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
                |   things(ids: ["thing-2:partition-B", "thing-4:partition-B"]) {
                |     id
                |     rename__name__underlyingName: underlyingName
                |     __typename__rename__name: __typename
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
                |         "rename__name__underlyingName": "THING-2",
                |         "__typename__rename__name": "Thing"
                |       },
                |       {
                |         "id": "thing-4",
                |         "rename__name__underlyingName": "THING-4",
                |         "__typename__rename__name": "Thing"
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
                |   things(ids: ["thing-5:partition-C", "thing-7:partition-C"]) {
                |     id
                |     rename__name__underlyingName: underlyingName
                |     __typename__rename__name: __typename
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "things": [
                |       {
                |         "id": "thing-5",
                |         "rename__name__underlyingName": "THING-5",
                |         "__typename__rename__name": "Thing"
                |       },
                |       {
                |         "id": "thing-7",
                |         "rename__name__underlyingName": "THING-7",
                |         "__typename__rename__name": "Thing"
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
                |   things(ids: ["thing-6:partition-D", "thing-8:partition-D"]) {
                |     id
                |     rename__name__underlyingName: underlyingName
                |     __typename__rename__name: __typename
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "things": [
                |       {
                |         "id": "thing-6",
                |         "rename__name__underlyingName": "THING-6",
                |         "__typename__rename__name": "Thing"
                |       },
                |       {
                |         "id": "thing-8",
                |         "rename__name__underlyingName": "THING-8",
                |         "__typename__rename__name": "Thing"
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
     *       },
     *       {
     *         "id": "thing-5",
     *         "name": "THING-5"
     *       },
     *       {
     *         "id": "thing-7",
     *         "name": "THING-7"
     *       },
     *       {
     *         "id": "thing-6",
     *         "name": "THING-6"
     *       },
     *       {
     *         "id": "thing-8",
     *         "name": "THING-8"
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
            |       },
            |       {
            |         "id": "thing-5",
            |         "name": "THING-5"
            |       },
            |       {
            |         "id": "thing-7",
            |         "name": "THING-7"
            |       },
            |       {
            |         "id": "thing-6",
            |         "name": "THING-6"
            |       },
            |       {
            |         "id": "thing-8",
            |         "name": "THING-8"
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
