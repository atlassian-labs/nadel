// @formatter:off
package graphql.nadel.tests.next.fixtures.hydration.copy

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<HydrationCopiesFieldAndHasPolymorphicHydrationTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class HydrationCopiesFieldAndHasPolymorphicHydrationTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            )

    /**
     * ```json
     * {
     *   "errors": [
     *     {
     *       "message": "Validation error
     * (InvalidFragmentType@[businessReport_findRecentWorkByTeam/edges/node]) : Fragment cannot be
     * spread here as objects of type 'WorkNode' can never be of type 'BitbucketPullRequest'",
     *       "locations": [
     *         {
     *           "line": 8,
     *           "column": 9
     *         }
     *       ],
     *       "extensions": {
     *         "classification": "ValidationError"
     *       }
     *     }
     *   ]
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "errors": [
            |     {
            |       "message": "Validation error (InvalidFragmentType@[businessReport_findRecentWorkByTeam/edges/node]) : Fragment cannot be spread here as objects of type 'WorkNode' can never be of type 'BitbucketPullRequest'",
            |       "locations": [
            |         {
            |           "line": 8,
            |           "column": 9
            |         }
            |       ],
            |       "extensions": {
            |         "classification": "ValidationError"
            |       }
            |     }
            |   ]
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
