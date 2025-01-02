// @formatter:off
package graphql.nadel.tests.legacy.scalars

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`hydrating using json data as arg`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `hydrating using json data as arg snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Baz",
                query = """
                | query (${'$'}v0: JSON!) {
                |   baz(data: ${'$'}v0) {
                |     id
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": {
                |     "id": "102",
                |     "appConfig": {
                |       "status": "active",
                |       "bounce": false
                |     }
                |   }
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "baz": {
                |       "id": "10000"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "service",
                query = """
                | query (${'$'}v0: JSON) {
                |   foo(input: ${'$'}v0) {
                |     __typename__hydration__foo: __typename
                |     hydration__foo__baz: baz
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": {
                |     "something": true,
                |     "answer": "42"
                |   }
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "foo": {
                |       "__typename__hydration__foo": "Foo",
                |       "hydration__foo__baz": {
                |         "id": "102",
                |         "appConfig": {
                |           "status": "active",
                |           "bounce": false
                |         }
                |       }
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
     *       "foo": {
     *         "id": "10000"
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
            |       "foo": {
            |         "id": "10000"
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
