// @formatter:off
package graphql.nadel.tests.legacy.`skip-include-fields`

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`handles include directive on single top level field`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `handles include directive on single top level field snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            )

    /**
     * ```json
     * {
     *   "data": {}
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {}
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
