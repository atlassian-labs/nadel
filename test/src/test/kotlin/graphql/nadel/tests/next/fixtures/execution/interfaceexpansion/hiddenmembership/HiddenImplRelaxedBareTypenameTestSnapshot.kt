// @formatter:off
package graphql.nadel.tests.next.fixtures.execution.interfaceexpansion.hiddenmembership

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<HiddenImplRelaxedBareTypenameTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class HiddenImplRelaxedBareTypenameTestSnapshot : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * query {
     *   nodes {
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
                service = "data",
                query = """
                | {
                |   nodes {
                |     __typename
                |     __typename__abstract_member__nodes: __typename
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "nodes": [
                |       {
                |         "__typename": "Issue",
                |         "__typename__abstract_member__nodes": "Issue"
                |       },
                |       {
                |         "__typename": "Story",
                |         "__typename__abstract_member__nodes": "Story"
                |       },
                |       {
                |         "__typename": "Task",
                |         "__typename__abstract_member__nodes": "Task"
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
     *     "nodes": [
     *       {
     *         "__typename": "Issue"
     *       },
     *       {
     *         "__typename": "Story"
     *       },
     *       {}
     *     ]
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "nodes": [
            |       {
            |         "__typename": "Issue"
            |       },
            |       {
            |         "__typename": "Story"
            |       },
            |       {}
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
