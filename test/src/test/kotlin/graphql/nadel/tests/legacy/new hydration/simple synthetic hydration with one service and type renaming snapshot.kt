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
    graphql.nadel.tests.next.update<`simple synthetic hydration with one service and type renaming`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `simple synthetic hydration with one service and type renaming snapshot` :
        TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "testing",
                query = """
                | {
                |   tests {
                |     character(id: "C1") {
                |       id
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
                |       "character": {
                |         "id": "C1",
                |         "name": "Luke"
                |       }
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
                | {
                |   tests {
                |     testing {
                |       movie {
                |         __typename__hydration__character: __typename
                |         hydration__character__characterId: characterId
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
                |         "movie": {
                |           "id": "M1",
                |           "name": "Movie 1",
                |           "hydration__character__characterId": "C1",
                |           "__typename__hydration__character": "Movie"
                |         }
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
     *         "movie": {
     *           "id": "M1",
     *           "name": "Movie 1",
     *           "character": {
     *             "id": "C1",
     *             "name": "Luke"
     *           }
     *         }
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
            |         "movie": {
            |           "id": "M1",
            |           "name": "Movie 1",
            |           "character": {
            |             "id": "C1",
            |             "name": "Luke"
            |           }
            |         }
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
