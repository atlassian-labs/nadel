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
    graphql.nadel.tests.next.update<PartitionWithTypeNameTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class PartitionWithTypeNameTestSnapshot : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * query getPartitionedThings{
     *   __typename
     *   things(ids: [
     *       "thing-1:partition-A", "thing-2:partition-B", "thing-3:partition-A",
     * "thing-4:partition-B",
     *       "thing-5:partition-C", "thing-6:partition-D", "thing-7:partition-C",
     * "thing-8:partition-D"
     *   ]) {
     *     __typename
     *     id
     *     name
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
                service = "__introspection",
                query = """
                | query getPartitionedThings {
                |   __typename
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "__typename": "Query"
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
                |   things(ids: ["thing-1:partition-A", "thing-3:partition-A"]) {
                |     __typename
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
                |         "__typename": "Thing",
                |         "id": "thing-1",
                |         "name": "THING-1"
                |       },
                |       {
                |         "__typename": "Thing",
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
                |   things(ids: ["thing-2:partition-B", "thing-4:partition-B"]) {
                |     __typename
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
                |         "__typename": "Thing",
                |         "id": "thing-2",
                |         "name": "THING-2"
                |       },
                |       {
                |         "__typename": "Thing",
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
            ExpectedServiceCall(
                service = "things_service",
                query = """
                | query getPartitionedThings {
                |   things(ids: ["thing-5:partition-C", "thing-7:partition-C"]) {
                |     __typename
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
                |         "__typename": "Thing",
                |         "id": "thing-5",
                |         "name": "THING-5"
                |       },
                |       {
                |         "__typename": "Thing",
                |         "id": "thing-7",
                |         "name": "THING-7"
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
                |     __typename
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
                |         "__typename": "Thing",
                |         "id": "thing-6",
                |         "name": "THING-6"
                |       },
                |       {
                |         "__typename": "Thing",
                |         "id": "thing-8",
                |         "name": "THING-8"
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
     *     "__typename": "Query",
     *     "things": [
     *       {
     *         "__typename": "Thing",
     *         "id": "thing-1",
     *         "name": "THING-1"
     *       },
     *       {
     *         "__typename": "Thing",
     *         "id": "thing-3",
     *         "name": "THING-3"
     *       },
     *       {
     *         "__typename": "Thing",
     *         "id": "thing-2",
     *         "name": "THING-2"
     *       },
     *       {
     *         "__typename": "Thing",
     *         "id": "thing-4",
     *         "name": "THING-4"
     *       },
     *       {
     *         "__typename": "Thing",
     *         "id": "thing-5",
     *         "name": "THING-5"
     *       },
     *       {
     *         "__typename": "Thing",
     *         "id": "thing-7",
     *         "name": "THING-7"
     *       },
     *       {
     *         "__typename": "Thing",
     *         "id": "thing-6",
     *         "name": "THING-6"
     *       },
     *       {
     *         "__typename": "Thing",
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
            |     "__typename": "Query",
            |     "things": [
            |       {
            |         "__typename": "Thing",
            |         "id": "thing-1",
            |         "name": "THING-1"
            |       },
            |       {
            |         "__typename": "Thing",
            |         "id": "thing-3",
            |         "name": "THING-3"
            |       },
            |       {
            |         "__typename": "Thing",
            |         "id": "thing-2",
            |         "name": "THING-2"
            |       },
            |       {
            |         "__typename": "Thing",
            |         "id": "thing-4",
            |         "name": "THING-4"
            |       },
            |       {
            |         "__typename": "Thing",
            |         "id": "thing-5",
            |         "name": "THING-5"
            |       },
            |       {
            |         "__typename": "Thing",
            |         "id": "thing-7",
            |         "name": "THING-7"
            |       },
            |       {
            |         "__typename": "Thing",
            |         "id": "thing-6",
            |         "name": "THING-6"
            |       },
            |       {
            |         "__typename": "Thing",
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
