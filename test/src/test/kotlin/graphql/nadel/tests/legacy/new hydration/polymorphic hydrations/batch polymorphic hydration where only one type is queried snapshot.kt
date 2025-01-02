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
    graphql.nadel.tests.next.update<`batch polymorphic hydration where only one type is queried`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `batch polymorphic hydration where only one type is queried snapshot` : TestSnapshot()
        {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "foo",
                query = """
                | {
                |   foo {
                |     __typename__batch_hydration__data: __typename
                |     batch_hydration__data__dataId: dataId
                |     batch_hydration__data__dataId: dataId
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "foo": [
                |       {
                |         "batch_hydration__data__dataId": "DOG-0",
                |         "__typename__batch_hydration__data": "Foo"
                |       },
                |       {
                |         "batch_hydration__data__dataId": "FISH-0",
                |         "__typename__batch_hydration__data": "Foo"
                |       },
                |       {
                |         "batch_hydration__data__dataId": "DOG-1",
                |         "__typename__batch_hydration__data": "Foo"
                |       },
                |       {
                |         "batch_hydration__data__dataId": "FISH-1",
                |         "__typename__batch_hydration__data": "Foo"
                |       },
                |       {
                |         "batch_hydration__data__dataId": "HUMAN-0",
                |         "__typename__batch_hydration__data": "Foo"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "people",
                query = """
                | {
                |   humanById(ids: ["HUMAN-0"]) {
                |     batch_hydration__data__id: id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "humanById": [
                |       {
                |         "batch_hydration__data__id": "HUMAN-0"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "pets",
                query = """
                | {
                |   petById(ids: ["DOG-0", "FISH-0", "DOG-1", "FISH-1"]) {
                |     ... on Dog {
                |       batch_hydration__data__id: id
                |     }
                |     ... on Fish {
                |       __typename
                |       fins
                |       id
                |       batch_hydration__data__id: id
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "petById": [
                |       {
                |         "batch_hydration__data__id": "DOG-0"
                |       },
                |       {
                |         "__typename": "Fish",
                |         "id": "FISH-0",
                |         "fins": 4,
                |         "batch_hydration__data__id": "FISH-0"
                |       },
                |       {
                |         "batch_hydration__data__id": "DOG-1"
                |       },
                |       {
                |         "__typename": "Fish",
                |         "id": "FISH-1",
                |         "fins": 8,
                |         "batch_hydration__data__id": "FISH-1"
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
     *     "foo": [
     *       {
     *         "data": {}
     *       },
     *       {
     *         "data": {
     *           "__typename": "Fish",
     *           "id": "FISH-0",
     *           "fins": 4
     *         }
     *       },
     *       {
     *         "data": {}
     *       },
     *       {
     *         "data": {
     *           "__typename": "Fish",
     *           "id": "FISH-1",
     *           "fins": 8
     *         }
     *       },
     *       {
     *         "data": {}
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
            |     "foo": [
            |       {
            |         "data": {}
            |       },
            |       {
            |         "data": {
            |           "__typename": "Fish",
            |           "id": "FISH-0",
            |           "fins": 4
            |         }
            |       },
            |       {
            |         "data": {}
            |       },
            |       {
            |         "data": {
            |           "__typename": "Fish",
            |           "id": "FISH-1",
            |           "fins": 8
            |         }
            |       },
            |       {
            |         "data": {}
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
