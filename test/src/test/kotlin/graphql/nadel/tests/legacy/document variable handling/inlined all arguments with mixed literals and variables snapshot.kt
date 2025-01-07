// @formatter:off
package graphql.nadel.tests.legacy.`document variable handling`

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`inlined all arguments with mixed literals and variables`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `inlined all arguments with mixed literals and variables snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "MyService",
                query = """
                | query myQuery(${'$'}v0: UnderlyingInputArgType) {
                |   hello(arg: ${'$'}v0)
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": {
                |     "age": 50,
                |     "inputWithJson": {
                |       "names": [
                |         "Bobba",
                |         "Fett"
                |       ],
                |       "payload": {
                |         "name": "Bobert",
                |         "age": "23"
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "hello": "world"
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
     *     "hello": "world"
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "hello": "world"
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
