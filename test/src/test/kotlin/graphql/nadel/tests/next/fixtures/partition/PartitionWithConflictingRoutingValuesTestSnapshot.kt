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
    graphql.nadel.tests.next.update<PartitionWithConflictingRoutingValuesTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class PartitionWithConflictingRoutingValuesTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            )

    /**
     * ```json
     * {
     *   "errors": [
     *     {
     *       "message": "The call for field 'things' was not partitioned due to the following error:
     * 'Expected only one partition key but got 2'",
     *       "locations": [],
     *       "path": [
     *         "things"
     *       ],
     *       "extensions": {
     *         "classification": "PartitioningError"
     *       }
     *     }
     *   ],
     *   "data": {
     *     "things": null
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "errors": [
            |     {
            |       "message": "The call for field 'things' was not partitioned due to the following error: 'Expected only one partition key but got 2'",
            |       "locations": [],
            |       "path": [
            |         "things"
            |       ],
            |       "extensions": {
            |         "classification": "PartitioningError"
            |       }
            |     }
            |   ],
            |   "data": {
            |     "things": null
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
