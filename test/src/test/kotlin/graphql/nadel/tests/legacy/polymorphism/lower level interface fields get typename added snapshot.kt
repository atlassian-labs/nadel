// @formatter:off
package graphql.nadel.tests.legacy.polymorphism

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`lower level interface fields get typename added`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `lower level interface fields get typename added snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "PetService",
                query = """
                | query petQ {
                |   pets(isLoyal: true) {
                |     collar {
                |       color
                |     }
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "pets": [
                |       {
                |         "name": "Sparky",
                |         "collar": {
                |           "color": "blue"
                |         }
                |       },
                |       {
                |         "name": "Whiskers",
                |         "collar": {
                |           "color": "red"
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
     *     "pets": [
     *       {
     *         "name": "Sparky",
     *         "collar": {
     *           "color": "blue"
     *         }
     *       },
     *       {
     *         "name": "Whiskers",
     *         "collar": {
     *           "color": "red"
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
            |     "pets": [
            |       {
            |         "name": "Sparky",
            |         "collar": {
            |           "color": "blue"
            |         }
            |       },
            |       {
            |         "name": "Whiskers",
            |         "collar": {
            |           "color": "red"
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
