// @formatter:off
package graphql.nadel.tests.legacy.`new hydration`.batching

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`batching conditional hydration accepts true condition after a false condition`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class
        `batching conditional hydration accepts true condition after a false condition snapshot` :
        TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "service1",
                query = """
                | query {
                |   foo {
                |     __typename__batch_hydration__bar: __typename
                |     batch_hydration__bar__barIds: barIds
                |     batch_hydration__bar__barIds: barIds
                |     batch_hydration__bar__type: type
                |     batch_hydration__bar__type: type
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "foo": {
                |       "__typename__batch_hydration__bar": "Foo",
                |       "batch_hydration__bar__barIds": [
                |         "barId1",
                |         "barId2"
                |       ],
                |       "batch_hydration__bar__type": "thisType"
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
                | query {
                |   othersById(ids: ["barId1", "barId2"]) {
                |     batch_hydration__bar__id: id
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "othersById": [
                |       {
                |         "batch_hydration__bar__id": "barId1",
                |         "name": "Bar1"
                |       },
                |       {
                |         "batch_hydration__bar__id": "barId2",
                |         "name": "Bar2"
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
