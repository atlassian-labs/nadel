// @formatter:off
package graphql.nadel.tests.legacy.introspection

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`introspection with variables FF on`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `introspection with variables FF on snapshot` : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * query {
     *   __schema {
     *     queryType {
     *       fields(includeDeprecated: true) {
     *         name
     *         isDeprecated
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
                service = "__introspection",
                query = """
                | query (${'$'}v0: Boolean!) {
                |   __schema {
                |     queryType {
                |       fields(includeDeprecated: ${'$'}v0) {
                |         isDeprecated
                |         name
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": true
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "__schema": {
                |       "queryType": {
                |         "fields": [
                |           {
                |             "name": "earth",
                |             "isDeprecated": false
                |           },
                |           {
                |             "name": "pluto",
                |             "isDeprecated": true
                |           }
                |         ]
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
     *     "__schema": {
     *       "queryType": {
     *         "fields": [
     *           {
     *             "name": "earth",
     *             "isDeprecated": false
     *           },
     *           {
     *             "name": "pluto",
     *             "isDeprecated": true
     *           }
     *         ]
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
            |     "__schema": {
            |       "queryType": {
            |         "fields": [
            |           {
            |             "name": "earth",
            |             "isDeprecated": false
            |           },
            |           {
            |             "name": "pluto",
            |             "isDeprecated": true
            |           }
            |         ]
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
