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
    graphql.nadel.tests.next.update<`introspection can be performed even with fragments`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `introspection can be performed even with fragments snapshot` : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * query {
     *   ...IntrospectionFrag
     *   __type(name: "World") {
     *     name
     *   }
     *   __typename
     * }
     * fragment IntrospectionFrag on Query {
     *   __schema {
     *     queryType {
     *       name
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
                | {
                |   __schema {
                |     queryType {
                |       name
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "__schema": {
                |       "queryType": {
                |         "name": "Query"
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "__introspection",
                query = """
                | {
                |   __type(name: "World") {
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "__type": {
                |       "name": "World"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "__introspection",
                query = """
                | {
                |   __typename
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "__typename": "Query"
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
     *         "name": "Query"
     *       }
     *     },
     *     "__type": {
     *       "name": "World"
     *     },
     *     "__typename": "Query"
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
            |         "name": "Query"
            |       }
            |     },
            |     "__type": {
            |       "name": "World"
            |     },
            |     "__typename": "Query"
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
