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
    graphql.nadel.tests.next.update<`hydration call forwards error`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `hydration call forwards error snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "service1",
                query = """
                | {
                |   foo {
                |     __typename__hydration__bar: __typename
                |     hydration__bar__barId: barId
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "foo": {
                |       "hydration__bar__barId": "barId1",
                |       "__typename__hydration__bar": "Foo"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "service2",
                query = """
                | {
                |   barById(id: "barId1") {
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "errors": [
                |     {
                |       "message": "Some error occurred",
                |       "locations": [],
                |       "extensions": {
                |         "classification": "DataFetchingException"
                |       }
                |     },
                |     {
                |       "message": "Some error with extension occurred",
                |       "locations": [],
                |       "path": [
                |         "barById",
                |         "name"
                |       ],
                |       "extensions": {
                |         "classification": "SomeCustomError"
                |       }
                |     }
                |   ],
                |   "data": {
                |     "barById": null
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
     *   "errors": [
     *     {
     *       "message": "Some error occurred",
     *       "locations": [],
     *       "extensions": {
     *         "classification": "DataFetchingException"
     *       }
     *     },
     *     {
     *       "message": "Some error with extension occurred",
     *       "locations": [],
     *       "path": [
     *         "barById",
     *         "name"
     *       ],
     *       "extensions": {
     *         "classification": "SomeCustomError"
     *       }
     *     }
     *   ],
     *   "data": {
     *     "foo": {
     *       "bar": null
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "errors": [
            |     {
            |       "message": "Some error occurred",
            |       "locations": [],
            |       "extensions": {
            |         "classification": "DataFetchingException"
            |       }
            |     },
            |     {
            |       "message": "Some error with extension occurred",
            |       "locations": [],
            |       "path": [
            |         "barById",
            |         "name"
            |       ],
            |       "extensions": {
            |         "classification": "SomeCustomError"
            |       }
            |     }
            |   ],
            |   "data": {
            |     "foo": {
            |       "bar": null
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
