// @formatter:off
package graphql.nadel.tests.legacy.oneOf

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`oneOf successful`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `oneOf successful snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "MyService",
                query = """
                | query myQuery(${'$'}v0: SearchInput) {
                |   search(by: ${'$'}v0)
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": {
                |     "name": "Figaro"
                |   }
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "search": "Figaro"
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
     *     "search": "Figaro"
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "search": "Figaro"
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
