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
    graphql.nadel.tests.next.update<MultiInterfaceHiddenImplRelaxedBareInterfaceFieldTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class MultiInterfaceHiddenImplRelaxedBareInterfaceFieldTestSnapshot : TestSnapshot() {
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
                |       __typename__abstract_member__item: __typename
                |       id
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
                |           "id": "WIKI-1",
                |           "__typename__abstract_member__item": "Wiki"
                |         }
                |       },
                |       {
                |         "item": {
                |           "id": "POST-1",
                |           "__typename__abstract_member__item": "Post"
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
