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
    graphql.nadel.tests.next.update<`synthetic hydration call over itself within renamed types`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `synthetic hydration call over itself within renamed types snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "testing",
                query = """
                | query {
                |   tests {
                |     characters(ids: ["C1", "C2", "C3"]) {
                |       id
                |       batch_hydration__characters__id: id
                |       name
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "tests": {
                |       "characters": [
                |         {
                |           "name": "Luke",
                |           "batch_hydration__characters__id": "C1",
                |           "id": "C1"
                |         },
                |         {
                |           "name": "Leia",
                |           "batch_hydration__characters__id": "C2",
                |           "id": "C2"
                |         },
                |         {
                |           "name": "Anakin",
                |           "batch_hydration__characters__id": "C3",
                |           "id": "C3"
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
                service = "testing",
                query = """
                | query {
                |   tests {
                |     testing {
                |       movies {
                |         __typename__batch_hydration__characters: __typename
                |         batch_hydration__characters__characterIds: characterIds
                |         id
                |         name
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "tests": {
                |       "testing": {
                |         "movies": [
                |           {
                |             "name": "Movie 1",
                |             "id": "M1",
                |             "batch_hydration__characters__characterIds": [
                |               "C1",
                |               "C2"
                |             ],
                |             "__typename__batch_hydration__characters": "Movie"
                |           },
                |           {
                |             "name": "Movie 2",
                |             "id": "M2",
                |             "batch_hydration__characters__characterIds": [
                |               "C1",
                |               "C2",
                |               "C3"
                |             ],
                |             "__typename__batch_hydration__characters": "Movie"
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
     *     "tests": {
     *       "testing": {
     *         "movies": [
     *           {
     *             "id": "M1",
     *             "name": "Movie 1",
     *             "characters": [
     *               {
     *                 "id": "C1",
     *                 "name": "Luke"
     *               },
     *               {
     *                 "id": "C2",
     *                 "name": "Leia"
     *               }
     *             ]
     *           },
     *           {
     *             "id": "M2",
     *             "name": "Movie 2",
     *             "characters": [
     *               {
     *                 "id": "C1",
     *                 "name": "Luke"
     *               },
     *               {
     *                 "id": "C2",
     *                 "name": "Leia"
     *               },
     *               {
     *                 "id": "C3",
     *                 "name": "Anakin"
     *               }
     *             ]
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
            |     "tests": {
            |       "testing": {
            |         "movies": [
            |           {
            |             "id": "M1",
            |             "name": "Movie 1",
            |             "characters": [
            |               {
            |                 "id": "C1",
            |                 "name": "Luke"
            |               },
            |               {
            |                 "id": "C2",
            |                 "name": "Leia"
            |               }
            |             ]
            |           },
            |           {
            |             "id": "M2",
            |             "name": "Movie 2",
            |             "characters": [
            |               {
            |                 "id": "C1",
            |                 "name": "Luke"
            |               },
            |               {
            |                 "id": "C2",
            |                 "name": "Leia"
            |               },
            |               {
            |                 "id": "C3",
            |                 "name": "Anakin"
            |               }
            |             ]
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
