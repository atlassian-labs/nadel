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
    graphql.nadel.tests.next.update<`lower level interface fields which are renamed get typename added`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `lower level interface fields which are renamed get typename added snapshot` :
        TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "PetService",
                query = """
                | query petQ {
                |   pets(isLoyal: true) {
                |     __typename__rename__collarToRenamed: __typename
                |     name
                |     ... on Cat {
                |       rename__collarToRenamed__collar: collar {
                |         color
                |       }
                |     }
                |     ... on Dog {
                |       rename__collarToRenamed__collar: collar {
                |         color
                |       }
                |     }
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
                |         "rename__collarToRenamed__collar": {
                |           "color": "blue"
                |         },
                |         "__typename__rename__collarToRenamed": "Dog"
                |       },
                |       {
                |         "name": "Whiskers",
                |         "rename__collarToRenamed__collar": {
                |           "color": "red"
                |         },
                |         "__typename__rename__collarToRenamed": "Cat"
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
     *         "collarToRenamed": {
     *           "color": "blue"
     *         }
     *       },
     *       {
     *         "name": "Whiskers",
     *         "collarToRenamed": {
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
            |         "collarToRenamed": {
            |           "color": "blue"
            |         }
            |       },
            |       {
            |         "name": "Whiskers",
            |         "collarToRenamed": {
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
