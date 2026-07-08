// @formatter:off
package graphql.nadel.tests.next.fixtures.execution.interfaceexpansion.renames

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<RenamedAncestorBareTypenameTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class RenamedAncestorBareTypenameTestSnapshot : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * query {
     *   container {
     *     things {
     *       __typename
     *     }
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
                service = "data",
                query = """
                | {
                |   container {
                |     __typename__rename__things: __typename
                |     rename__things__nodes: nodes {
                |       ... on Issue {
                |         __typename
                |       }
                |       ... on Story {
                |         __typename
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "container": {
                |       "rename__things__nodes": [
                |         {
                |           "__typename": "Issue"
                |         },
                |         {
                |           "__typename": "Story"
                |         },
                |         {}
                |       ],
                |       "__typename__rename__things": "Container"
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
     *     "container": {
     *       "things": [
     *         {
     *           "__typename": "Issue"
     *         },
     *         {
     *           "__typename": "Story"
     *         },
     *         {}
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
            |     "container": {
            |       "things": [
            |         {
            |           "__typename": "Issue"
            |         },
            |         {
            |           "__typename": "Story"
            |         },
            |         {}
            |       ]
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
