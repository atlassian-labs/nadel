// @formatter:off
package graphql.nadel.tests.next.fixtures.execution.interfaceexpansion.multiinterface

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<MultiInterfaceHiddenImplBareInterfaceFieldTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class MultiInterfaceHiddenImplBareInterfaceFieldTestSnapshot : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * query {
     *   containers {
     *     item {
     *       id
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
                service = "data",
                query = """
                | {
                |   containers {
                |     item {
                |       ... on Post {
                |         id
                |       }
                |       ... on Wiki {
                |         id
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "containers": [
                |       {
                |         "item": {
                |           "id": "WIKI-1"
                |         }
                |       },
                |       {
                |         "item": {
                |           "id": "POST-1"
                |         }
                |       }
                |     ]
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
     *     "containers": [
     *       {
     *         "item": {
     *           "id": "WIKI-1"
     *         }
     *       },
     *       {
     *         "item": {
     *           "id": "POST-1"
     *         }
     *       }
     *     ]
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "containers": [
            |       {
            |         "item": {
            |           "id": "WIKI-1"
            |         }
            |       },
            |       {
            |         "item": {
            |           "id": "POST-1"
            |         }
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
