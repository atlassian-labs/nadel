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
    graphql.nadel.tests.next.update<`batch hydration with renamed actor field`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `batch hydration with renamed actor field snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "service1",
                query = """
                | {
                |   foo {
                |     __typename__batch_hydration__bar: __typename
                |     batch_hydration__bar__barId: barId
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "foo": {
                |       "batch_hydration__bar__barId": [
                |         "barId1",
                |         "barId2",
                |         "barId3"
                |       ],
                |       "__typename__batch_hydration__bar": "Foo"
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
                |   rename__barsByIdOverall__barsById: barsById(id: ["barId1", "barId2", "barId3"]) {
                |     batch_hydration__bar__id: id
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "rename__barsByIdOverall__barsById": [
                |       {
                |         "name": "Bar1",
                |         "batch_hydration__bar__id": "barId1"
                |       },
                |       {
                |         "name": "Bar2",
                |         "batch_hydration__bar__id": "barId2"
                |       },
                |       {
                |         "name": "Bar3",
                |         "batch_hydration__bar__id": "barId3"
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
     *     "foo": {
     *       "bar": [
     *         {
     *           "name": "Bar1"
     *         },
     *         {
     *           "name": "Bar2"
     *         },
     *         {
     *           "name": "Bar3"
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
            |     "foo": {
            |       "bar": [
            |         {
            |           "name": "Bar1"
            |         },
            |         {
            |           "name": "Bar2"
            |         },
            |         {
            |           "name": "Bar3"
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
