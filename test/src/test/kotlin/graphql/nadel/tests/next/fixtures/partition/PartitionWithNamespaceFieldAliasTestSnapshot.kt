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
    graphql.nadel.tests.next.update<PartitionWithNamespaceFieldAliasTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class PartitionWithNamespaceFieldAliasTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "things_service",
                query = """
                | query getPartitionedThings(${'$'}v0: [ID!]!) {
                |   aliasedNamespace: namespace {
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
                |   ]
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "aliasedNamespace": {
                |       "things": [
                |         {
                |           "id": "thing-1",
                |           "name": "THING-1"
                |         },
                |         {
                |           "id": "thing-3",
                |           "name": "THING-3"
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
                service = "things_service",
                query = """
                | query getPartitionedThings(${'$'}v0: [ID!]!) {
                |   aliasedNamespace: namespace {
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
                |     "aliasedNamespace": {
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
            ExpectedServiceCall(
                service = "things_service",
                query = """
                | query getPartitionedThings(${'$'}v0: [ID!]!) {
                |   aliasedNamespace: namespace {
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
                |     "thing-5:partition-C",
                |     "thing-7:partition-C"
                |   ]
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "aliasedNamespace": {
                |       "things": [
                |         {
                |           "id": "thing-5",
                |           "name": "THING-5"
                |         },
                |         {
                |           "id": "thing-7",
                |           "name": "THING-7"
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
                service = "things_service",
                query = """
                | query getPartitionedThings(${'$'}v0: [ID!]!) {
                |   aliasedNamespace: namespace {
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
                |     "thing-6:partition-D",
                |     "thing-8:partition-D"
                |   ]
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "aliasedNamespace": {
                |       "things": [
                |         {
                |           "id": "thing-6",
                |           "name": "THING-6"
                |         },
                |         {
                |           "id": "thing-8",
                |           "name": "THING-8"
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
     *     "aliasedNamespace": {
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
     *         },
     *         {
     *           "id": "thing-5",
     *           "name": "THING-5"
     *         },
     *         {
     *           "id": "thing-7",
     *           "name": "THING-7"
     *         },
     *         {
     *           "id": "thing-6",
     *           "name": "THING-6"
     *         },
     *         {
     *           "id": "thing-8",
     *           "name": "THING-8"
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
            |     "aliasedNamespace": {
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
            |         },
            |         {
            |           "id": "thing-5",
            |           "name": "THING-5"
            |         },
            |         {
            |           "id": "thing-7",
            |           "name": "THING-7"
            |         },
            |         {
            |           "id": "thing-6",
            |           "name": "THING-6"
            |         },
            |         {
            |           "id": "thing-8",
            |           "name": "THING-8"
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
