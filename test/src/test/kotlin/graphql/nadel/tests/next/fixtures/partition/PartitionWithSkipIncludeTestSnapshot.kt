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
    graphql.nadel.tests.next.update<PartitionWithSkipIncludeTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class PartitionWithSkipIncludeTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "things_service",
                query = """
                | query getPartitionedThings {
                |   things(ids: ["thing-1:partition-A", "thing-3:partition-A"]) {
                |     age
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "things": [
                |       {
                |         "age": 10
                |       },
                |       {
                |         "age": 10
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
                |     age
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "things": [
                |       {
                |         "age": 10
                |       },
                |       {
                |         "age": 10
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
                |     age
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "things": [
                |       {
                |         "age": 10
                |       },
                |       {
                |         "age": 10
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
                |     age
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "things": [
                |       {
                |         "age": 10
                |       },
                |       {
                |         "age": 10
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
     *         "age": 10
     *       },
     *       {
     *         "age": 10
     *       },
     *       {
     *         "age": 10
     *       },
     *       {
     *         "age": 10
     *       },
     *       {
     *         "age": 10
     *       },
     *       {
     *         "age": 10
     *       },
     *       {
     *         "age": 10
     *       },
     *       {
     *         "age": 10
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
            |         "age": 10
            |       },
            |       {
            |         "age": 10
            |       },
            |       {
            |         "age": 10
            |       },
            |       {
            |         "age": 10
            |       },
            |       {
            |         "age": 10
            |       },
            |       {
            |         "age": 10
            |       },
            |       {
            |         "age": 10
            |       },
            |       {
            |         "age": 10
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
