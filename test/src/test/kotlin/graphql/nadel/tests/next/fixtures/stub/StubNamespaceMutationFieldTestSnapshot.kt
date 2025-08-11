// @formatter:off
package graphql.nadel.tests.next.fixtures.stub

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<StubNamespaceMutationFieldTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class StubNamespaceMutationFieldTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            )

    /**
     * ```json
     * {
     *   "data": {
     *     "jira": {
     *       "createLlmBackedIssue": null
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "jira": {
            |       "createLlmBackedIssue": null
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
