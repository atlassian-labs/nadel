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
    graphql.nadel.tests.next.update<`query with three nested hydrations and synthetic fields`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `query with three nested hydrations and synthetic fields snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Bar",
                query = """
                | query {
                |   barsQuery {
                |     barsById(id: ["bar1", "bar2"]) {
                |       __typename__batch_hydration__nestedBar: __typename
                |       batch_hydration__bar__barId: barId
                |       name
                |       batch_hydration__nestedBar__nestedBarId: nestedBarId
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "barsQuery": {
                |       "barsById": [
                |         {
                |           "name": "Bar 1",
                |           "batch_hydration__bar__barId": "bar1",
                |           "batch_hydration__nestedBar__nestedBarId": "nestedBar1",
                |           "__typename__batch_hydration__nestedBar": "Bar"
                |         },
                |         {
                |           "name": "Bar 2",
                |           "batch_hydration__bar__barId": "bar2",
                |           "batch_hydration__nestedBar__nestedBarId": "nestedBar2",
                |           "__typename__batch_hydration__nestedBar": "Bar"
                |         }
                |       ]
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Bar",
                query = """
                | query {
                |   barsQuery {
                |     barsById(id: ["bar3"]) {
                |       __typename__batch_hydration__nestedBar: __typename
                |       batch_hydration__bar__barId: barId
                |       name
                |       batch_hydration__nestedBar__nestedBarId: nestedBarId
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "barsQuery": {
                |       "barsById": [
                |         {
                |           "name": "Bar 3",
                |           "batch_hydration__bar__barId": "bar3",
                |           "batch_hydration__nestedBar__nestedBarId": null,
                |           "__typename__batch_hydration__nestedBar": "Bar"
                |         }
                |       ]
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Bar",
                query = """
                | query {
                |   barsQuery {
                |     barsById(id: ["nestedBar1", "nestedBar2"]) {
                |       __typename__batch_hydration__nestedBar: __typename
                |       batch_hydration__nestedBar__barId: barId
                |       name
                |       batch_hydration__nestedBar__nestedBarId: nestedBarId
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "barsQuery": {
                |       "barsById": [
                |         {
                |           "name": "NestedBarName1",
                |           "batch_hydration__nestedBar__barId": "nestedBar1",
                |           "batch_hydration__nestedBar__nestedBarId": "nestedBarId456",
                |           "__typename__batch_hydration__nestedBar": "Bar"
                |         }
                |       ]
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Bar",
                query = """
                | query {
                |   barsQuery {
                |     barsById(id: ["nestedBarId456"]) {
                |       batch_hydration__nestedBar__barId: barId
                |       name
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "barsQuery": {
                |       "barsById": [
                |         {
                |           "name": "NestedBarName2",
                |           "batch_hydration__nestedBar__barId": "nestedBarId456"
                |         }
                |       ]
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
                | query {
                |   foos {
                |     __typename__batch_hydration__bar: __typename
                |     batch_hydration__bar__barId: barId
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "foos": [
                |       {
                |         "__typename__batch_hydration__bar": "Foo",
                |         "batch_hydration__bar__barId": "bar1"
                |       },
                |       {
                |         "__typename__batch_hydration__bar": "Foo",
                |         "batch_hydration__bar__barId": "bar2"
                |       },
                |       {
                |         "__typename__batch_hydration__bar": "Foo",
                |         "batch_hydration__bar__barId": "bar3"
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
     *     "foos": [
     *       {
     *         "bar": {
     *           "name": "Bar 1",
     *           "nestedBar": {
     *             "name": "NestedBarName1",
     *             "nestedBar": {
     *               "name": "NestedBarName2"
     *             }
     *           }
     *         }
     *       },
     *       {
     *         "bar": {
     *           "name": "Bar 2",
     *           "nestedBar": null
     *         }
     *       },
     *       {
     *         "bar": {
     *           "name": "Bar 3",
     *           "nestedBar": null
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
            |     "foos": [
            |       {
            |         "bar": {
            |           "name": "Bar 1",
            |           "nestedBar": {
            |             "name": "NestedBarName1",
            |             "nestedBar": {
            |               "name": "NestedBarName2"
            |             }
            |           }
            |         }
            |       },
            |       {
            |         "bar": {
            |           "name": "Bar 2",
            |           "nestedBar": null
            |         }
            |       },
            |       {
            |         "bar": {
            |           "name": "Bar 3",
            |           "nestedBar": null
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
