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
    graphql.nadel.tests.next.update<`inlined all arguments`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `inlined all arguments snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "MyService",
                query = """
                | query myQuery(${'$'}v0: InputWithJsonUnderlying, ${'$'}v1: JSON!, ${'$'}v2: String, ${'$'}v3: String, ${'$'}v4: String) {
                |   hello(arg: ${'$'}v0, arg1: ${'$'}v1, arg2: ${'$'}v2, arg3: ${'$'}v3, arg4Nullable: ${'$'}v4)
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": {
                |     "names": [
                |       "Bobba",
                |       "Fett"
                |     ],
                |     "payload": {
                |       "name": "Bobert",
                |       "age": "23"
                |     }
                |   },
                |   "v1": {
                |     "interests": [
                |       "photography",
                |       "basketball"
                |     ]
                |   },
                |   "v2": null,
                |   "v3": "input1",
                |   "v4": "defaulted"
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
