// @formatter:off
package graphql.nadel.tests.next.fixtures.batchHydration

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<BatchHydrationAtQueryTypeTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class BatchHydrationAtQueryTypeTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "issues",
                query = """
                | {
                |   batch_hydration__myIssues__myIssueKeys: myIssueKeys
                |   __typename__batch_hydration__myIssues: __typename
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "batch_hydration__myIssues__myIssueKeys": [
                |       "hello",
                |       "bye"
                |     ],
                |     "__typename__batch_hydration__myIssues": "Query"
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "issues",
                query = """
                | {
                |   issuesByIds(ids: ["hello", "bye"]) {
                |     title
                |     batch_hydration__myIssues__id: id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issuesByIds": [
                |       {
                |         "title": "Hello there",
                |         "batch_hydration__myIssues__id": "hello"
                |       },
                |       {
                |         "title": "Farewell",
                |         "batch_hydration__myIssues__id": "bye"
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
     *     "myIssues": [
     *       {
     *         "title": "Hello there"
     *       },
     *       {
     *         "title": "Farewell"
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
            |     "myIssues": [
            |       {
            |         "title": "Hello there"
            |       },
            |       {
            |         "title": "Farewell"
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
