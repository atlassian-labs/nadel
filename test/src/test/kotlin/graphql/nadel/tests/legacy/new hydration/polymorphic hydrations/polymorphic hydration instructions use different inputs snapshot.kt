// @formatter:off
package graphql.nadel.tests.legacy.`new hydration`.`polymorphic hydrations`

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`polymorphic hydration instructions use different inputs`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `polymorphic hydration instructions use different inputs snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Dogs",
                query = """
                | {
                |   dogsByIds(ids: ["good-boye-1"]) {
                |     __typename
                |     batch_hydration__animal__id: id
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "dogsByIds": [
                |       {
                |         "__typename": "Dog",
                |         "name": "Abe",
                |         "batch_hydration__animal__id": "good-boye-1"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Pets",
                query = """
                | {
                |   petsByIds(ids: ["good-boye-1", "tall-boye-9"]) {
                |     __typename__batch_hydration__animal: __typename
                |     batch_hydration__animal__animalId: animalId
                |     batch_hydration__animal__animalId: animalId
                |     batch_hydration__animal__giraffeInput: giraffeInput {
                |       nickname
                |     }
                |     batch_hydration__animal__giraffeInput: giraffeInput {
                |       birthday
                |     }
                |     batch_hydration__animal__giraffeInput: giraffeInput {
                |       height
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "petsByIds": [
                |       {
                |         "batch_hydration__animal__animalId": "good-boye-1",
                |         "batch_hydration__animal__giraffeInput": null,
                |         "__typename__batch_hydration__animal": "Pet"
                |       },
                |       {
                |         "batch_hydration__animal__animalId": "tall-boye-9",
                |         "batch_hydration__animal__giraffeInput": {
                |           "nickname": "Tall Boye",
                |           "birthday": 1001203200,
                |           "height": 570
                |         },
                |         "__typename__batch_hydration__animal": "Pet"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Zoo",
                query = """
                | {
                |   giraffes(filters: [{birthday : 1001203200, height : 570, nickname : "Tall Boye"}]) {
                |     __typename
                |     birthday
                |     batch_hydration__animal__birthday: birthday
                |     height
                |     batch_hydration__animal__height: height
                |     name
                |     batch_hydration__animal__nickname: nickname
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "giraffes": [
                |       {
                |         "__typename": "Giraffe",
                |         "name": "Rukiya",
                |         "birthday": 1001203200,
                |         "height": 570,
                |         "batch_hydration__animal__nickname": "Tall Boye",
                |         "batch_hydration__animal__birthday": 1001203200,
                |         "batch_hydration__animal__height": 570
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
     *     "petsByIds": [
     *       {
     *         "animal": {
     *           "__typename": "Dog",
     *           "name": "Abe"
     *         }
     *       },
     *       {
     *         "animal": {
     *           "__typename": "Giraffe",
     *           "name": "Rukiya",
     *           "birthday": 1001203200,
     *           "height": 570
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
            |     "petsByIds": [
            |       {
            |         "animal": {
            |           "__typename": "Dog",
            |           "name": "Abe"
            |         }
            |       },
            |       {
            |         "animal": {
            |           "__typename": "Giraffe",
            |           "name": "Rukiya",
            |           "birthday": 1001203200,
            |           "height": 570
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
