// @formatter:off
package graphql.nadel.tests.next.fixtures.execution

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<HiddenUnionMembersNamedFragmentTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class HiddenUnionMembersNamedFragmentTestSnapshot : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * query {
     *   abstract {
     *     ...Frag
     *   }
     * }
     * fragment Frag on Issue {
     *   key
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
            )

    /**
     * Combined Result
     *
     * ```json
     * {
     *   "errors": [
     *     {
     *       "message": "Validation error (InvalidFragmentType@[abstract]) : Fragment 'Frag' cannot
     * be spread here as objects of type 'Abstract' can never be of type 'Issue'",
     *       "locations": [
     *         {
     *           "line": 3,
     *           "column": 5
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
            |       "message": "Validation error (InvalidFragmentType@[abstract]) : Fragment 'Frag' cannot be spread here as objects of type 'Abstract' can never be of type 'Issue'",
            |       "locations": [
            |         {
            |           "line": 3,
            |           "column": 5
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
