// @formatter:off
package graphql.nadel.tests.legacy.renames

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`chain rename transform`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `chain rename transform snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "MyService",
                query = """
                | {
                |   rename__test__world: world(arg: "aaarrg") {
                |     __typename
                |     __typename__rename__cities: __typename
                |     id
                |     rename__cities__places: places(continent: Asia)
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "rename__test__world": {
                |       "__typename": "World",
                |       "id": "Earth",
                |       "rename__cities__places": [
                |         "Uhh yea I know cities"
                |       ],
                |       "__typename__rename__cities": "World"
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
     *     "test": {
     *       "__typename": "World",
     *       "id": "Earth",
     *       "cities": [
     *         "Uhh yea I know cities"
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
            |     "test": {
            |       "__typename": "World",
            |       "id": "Earth",
            |       "cities": [
            |         "Uhh yea I know cities"
            |       ]
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
