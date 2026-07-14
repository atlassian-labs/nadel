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
    graphql.nadel.tests.next.update<RenamedHiddenImplRelaxedBareInterfaceFieldTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class RenamedHiddenImplRelaxedBareInterfaceFieldTestSnapshot : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * query {
     *   nodes {
     *     id
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
                |     __typename__abstract_member__nodes: __typename
                |     id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "nodes": [
                |       {
                |         "id": "ISSUE-1",
                |         "__typename__abstract_member__nodes": "Issue"
                |       },
                |       {
                |         "id": "STORY-1",
                |         "__typename__abstract_member__nodes": "Story"
                |       },
                |       {
                |         "id": "SECRET-1",
                |         "__typename__abstract_member__nodes": "Secret"
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
     *         "id": "ISSUE-1"
     *       },
     *       {
     *         "id": "STORY-1"
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
            |         "id": "ISSUE-1"
            |       },
            |       {
            |         "id": "STORY-1"
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
