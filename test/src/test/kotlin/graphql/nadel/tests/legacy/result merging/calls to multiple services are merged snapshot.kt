// @formatter:off
package graphql.nadel.tests.legacy.`result merging`

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`calls to multiple services are merged`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `calls to multiple services are merged snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "bar",
                query = """
                | query (${'$'}v0: ID!) {
                |   bar(id: ${'$'}v0) {
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "1"
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "bar": {
                |       "name": "Bart"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "foo",
                query = """
                | query (${'$'}v0: ID!) {
                |   foo(id: ${'$'}v0) {
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "1"
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "foo": {
                |       "name": "Hello"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "foo",
                query = """
                | query (${'$'}v0: ID!) {
                |   loot: foo(id: ${'$'}v0) {
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "1"
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "loot": {
                |       "name": "World"
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
     *     "loot": {
     *       "name": "World"
     *     },
     *     "foo": {
     *       "name": "Hello"
     *     },
     *     "bar": {
     *       "name": "Bart"
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "loot": {
            |       "name": "World"
            |     },
            |     "foo": {
            |       "name": "Hello"
            |     },
            |     "bar": {
            |       "name": "Bart"
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
