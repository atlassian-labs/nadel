// @formatter:off
package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`hydration inside a renamed field`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `hydration inside a renamed field snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Bar",
                query = """
                | {
                |   barById(id: "hydrated-bar") {
                |     id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "barById": {
                |       "id": "hydrated-bar"
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
                |   rename__foo__fooOriginal: fooOriginal {
                |     __typename__hydration__fooBar: __typename
                |     hydration__fooBar__fooBarId: fooBarId
                |     id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "rename__foo__fooOriginal": {
                |       "id": "Foo",
                |       "hydration__fooBar__fooBarId": "hydrated-bar",
                |       "__typename__hydration__fooBar": "Foo"
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
     *     "foo": {
     *       "id": "Foo",
     *       "fooBar": {
     *         "id": "hydrated-bar"
     *       }
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "foo": {
            |       "id": "Foo",
            |       "fooBar": {
            |         "id": "hydrated-bar"
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
