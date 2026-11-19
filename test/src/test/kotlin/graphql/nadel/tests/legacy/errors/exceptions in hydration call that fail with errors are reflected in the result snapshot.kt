// @formatter:off
package graphql.nadel.tests.legacy.errors

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`exceptions in hydration call that fail with errors are reflected in the result`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class
        `exceptions in hydration call that fail with errors are reflected in the result snapshot` :
        TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * query {
     *   foo {
     *     bar {
     *       name
     *       nestedBar {
     *         name
     *         nestedBar {
     *           name
     *         }
     *       }
     *     }
     *   }
     * }
     * ```
     *
     * Variables
     *
     * ```json
     * {}
     * ```
     */
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Foo",
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
                |       "hydration__bar__barId": "barId123",
                |       "__typename__hydration__bar": "Foo"
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
     *   "errors": [
     *     {
     *       "message": "An PopGoesTheWeaselException occurred invoking the service Bar",
     *       "locations": [],
     *       "extensions": {
     *         "executionId": "test",
     *         "classification": "NadelUncaughtExecutionError"
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
            |       "message": "An PopGoesTheWeaselException occurred invoking the service Bar",
            |       "locations": [],
            |       "extensions": {
            |         "executionId": "test",
            |         "classification": "NadelUncaughtExecutionError"
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
