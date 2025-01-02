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
    graphql.nadel.tests.next.update<`query with hydrated interfaces work as expected`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `query with hydrated interfaces work as expected snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "OwnerService",
                query = """
                | query petQ {
                |   ownerById(id: "cruella") {
                |     name
                |     ... on CaringOwner {
                |       givesPats
                |     }
                |     ... on CruelOwner {
                |       givesSmacks
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "ownerById": {
                |       "name": "Cruella De Vil",
                |       "givesSmacks": true
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "OwnerService",
                query = """
                | query petQ {
                |   ownerById(id: "dearly") {
                |     name
                |     ... on CaringOwner {
                |       givesPats
                |     }
                |     ... on CruelOwner {
                |       givesSmacks
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "ownerById": {
                |       "name": "Mr Dearly",
                |       "givesPats": true
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "PetService",
                query = """
                | query petQ {
                |   pets(isLoyal: true) {
                |     __typename__hydration__owners: __typename
                |     name
                |     ... on Cat {
                |       hydration__owners__ownerIds: ownerIds
                |     }
                |     ... on Dog {
                |       hydration__owners__ownerIds: ownerIds
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
                |         "hydration__owners__ownerIds": [
                |           "dearly"
                |         ],
                |         "__typename__hydration__owners": "Dog"
                |       },
                |       {
                |         "name": "Whiskers",
                |         "hydration__owners__ownerIds": [
                |           "cruella"
                |         ],
                |         "__typename__hydration__owners": "Cat"
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
     *         "owners": [
     *           {
     *             "name": "Mr Dearly",
     *             "givesPats": true
     *           }
     *         ]
     *       },
     *       {
     *         "name": "Whiskers",
     *         "owners": [
     *           {
     *             "name": "Cruella De Vil",
     *             "givesSmacks": true
     *           }
     *         ]
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
            |         "owners": [
            |           {
            |             "name": "Mr Dearly",
            |             "givesPats": true
            |           }
            |         ]
            |       },
            |       {
            |         "name": "Whiskers",
            |         "owners": [
            |           {
            |             "name": "Cruella De Vil",
            |             "givesSmacks": true
            |           }
            |         ]
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
