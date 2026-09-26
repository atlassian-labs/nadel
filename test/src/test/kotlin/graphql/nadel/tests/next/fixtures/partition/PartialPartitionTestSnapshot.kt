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
    graphql.nadel.tests.next.update<PartialPartitionTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class PartialPartitionTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "things_service",
                query = """
                | query getPartitionedThings(${'$'}v0: [ID!]!, ${'$'}v1: ID!) {
                |   api {
                |     stuff(id: ${'$'}v1) {
                |       id
                |       name
                |     }
                |     things(ids: ${'$'}v0) {
                |       id
                |       name
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": [
                |     "thing-1:partition-A",
                |     "thing-3:partition-A"
                |   ],
                |   "v1": "Stuff-1"
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "api": {
                |       "things": [
                |         {
                |           "id": "thing-1",
                |           "name": "THING-1"
                |         },
                |         {
                |           "id": "thing-3",
                |           "name": "THING-3"
                |         }
                |       ],
                |       "stuff": {
                |         "id": "Stuff-1",
                |         "name": "STUFF-1"
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "things_service",
                query = """
                | query getPartitionedThings(${'$'}v0: [ID!]!) {
                |   api {
                |     things(ids: ${'$'}v0) {
                |       id
                |       name
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": [
                |     "thing-2:partition-B",
                |     "thing-4:partition-B"
                |   ]
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "api": {
                |       "things": [
                |         {
                |           "id": "thing-2",
                |           "name": "THING-2"
                |         },
                |         {
                |           "id": "thing-4",
                |           "name": "THING-4"
                |         }
                |       ]
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
     *     "api": {
     *       "things": [
     *         {
     *           "id": "thing-1",
     *           "name": "THING-1"
     *         },
     *         {
     *           "id": "thing-3",
     *           "name": "THING-3"
     *         },
     *         {
     *           "id": "thing-2",
     *           "name": "THING-2"
     *         },
     *         {
     *           "id": "thing-4",
     *           "name": "THING-4"
     *         }
     *       ],
     *       "stuff": {
     *         "id": "Stuff-1",
     *         "name": "STUFF-1"
     *       }
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "api": {
            |       "things": [
            |         {
            |           "id": "thing-1",
            |           "name": "THING-1"
            |         },
            |         {
            |           "id": "thing-3",
            |           "name": "THING-3"
            |         },
            |         {
            |           "id": "thing-2",
            |           "name": "THING-2"
            |         },
            |         {
            |           "id": "thing-4",
            |           "name": "THING-4"
            |         }
            |       ],
            |       "stuff": {
            |         "id": "Stuff-1",
            |         "name": "STUFF-1"
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
