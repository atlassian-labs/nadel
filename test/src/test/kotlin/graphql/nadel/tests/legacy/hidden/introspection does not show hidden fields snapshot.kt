// @formatter:off
package graphql.nadel.tests.legacy.hidden

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`introspection does not show hidden fields`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `introspection does not show hidden fields snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            )

    /**
     * ```json
     * {
     *   "data": {
     *     "__schema": {
     *       "queryType": {
     *         "fields": [
     *           {
     *             "name": "hello"
     *           }
     *         ]
     *       }
     *     },
     *     "__type": {
     *       "name": "World",
     *       "fields": [
     *         {
     *           "name": "id"
     *         },
     *         {
     *           "name": "name"
     *         }
     *       ]
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "__schema": {
            |       "queryType": {
            |         "fields": [
            |           {
            |             "name": "hello"
            |           }
            |         ]
            |       }
            |     },
            |     "__type": {
            |       "name": "World",
            |       "fields": [
            |         {
            |           "name": "id"
            |         },
            |         {
            |           "name": "name"
            |         }
            |       ]
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
