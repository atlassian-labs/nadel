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
    graphql.nadel.tests.next.update<RenamedAncestorBareInterfaceFieldTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class RenamedAncestorBareInterfaceFieldTestSnapshot : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * query {
     *   container {
     *     things {
     *       id
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
                |         id
                |       }
                |       ... on Story {
                |         id
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
                |           "id": "ISSUE-1"
                |         },
                |         {
                |           "id": "STORY-1"
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
     *           "id": "ISSUE-1"
     *         },
     *         {
     *           "id": "STORY-1"
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
            |           "id": "ISSUE-1"
            |         },
            |         {
            |           "id": "STORY-1"
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
