// @formatter:off
package graphql.nadel.tests.legacy.renames

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`query to two services with field rename`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `query to two services with field rename snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Bar",
                query = """
                | {
                |   bar {
                |     __typename__rename__name: __typename
                |     rename__name__title: title
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "bar": {
                |       "rename__name__title": "Bar",
                |       "__typename__rename__name": "Bar"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Foo",
                query = """
                | {
                |   rename__otherFoo__fooOriginal: fooOriginal {
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "rename__otherFoo__fooOriginal": {
                |       "name": "Foo"
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
     *     "otherFoo": {
     *       "name": "Foo"
     *     },
     *     "bar": {
     *       "name": "Bar"
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "otherFoo": {
            |       "name": "Foo"
            |     },
            |     "bar": {
            |       "name": "Bar"
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
