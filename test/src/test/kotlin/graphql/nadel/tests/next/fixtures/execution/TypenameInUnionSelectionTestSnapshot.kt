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
    graphql.nadel.tests.next.update<TypenameInUnionSelectionTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class TypenameInUnionSelectionTestSnapshot : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * query {
     *   abstract {
     *     __typename
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
                service = "abstract",
                query = """
                | {
                |   abstract {
                |     __typename
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "abstract": [
                |       {
                |         "__typename": "User"
                |       },
                |       {
                |         "__typename": "Issue"
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
     * Combined Result
     *
     * ```json
     * {
     *   "data": {
     *     "abstract": [
     *       {
     *         "__typename": "User"
     *       },
     *       {
     *         "__typename": "Issue"
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
            |     "abstract": [
            |       {
            |         "__typename": "User"
            |       },
            |       {
            |         "__typename": "Issue"
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
