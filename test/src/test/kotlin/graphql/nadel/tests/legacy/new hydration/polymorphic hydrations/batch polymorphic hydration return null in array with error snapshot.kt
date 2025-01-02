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
    graphql.nadel.tests.next.update<`batch polymorphic hydration return null in array with error`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `batch polymorphic hydration return null in array with error snapshot` : TestSnapshot()
        {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "foo",
                query = """
                | {
                |   foo {
                |     __typename
                |     __typename__batch_hydration__data: __typename
                |     batch_hydration__data__dataId: dataId
                |     batch_hydration__data__dataId: dataId
                |     id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "foo": [
                |       {
                |         "__typename": "Foo",
                |         "__typename__batch_hydration__data": "Foo",
                |         "batch_hydration__data__dataId": "PET-0",
                |         "id": "FOO-0"
                |       },
                |       {
                |         "__typename": "Foo",
                |         "__typename__batch_hydration__data": "Foo",
                |         "batch_hydration__data__dataId": "HUMAN-0",
                |         "id": "FOO-1"
                |       },
                |       {
                |         "__typename": "Foo",
                |         "__typename__batch_hydration__data": "Foo",
                |         "batch_hydration__data__dataId": "PET-1",
                |         "id": "FOO-2"
                |       },
                |       {
                |         "__typename": "Foo",
                |         "__typename__batch_hydration__data": "Foo",
                |         "batch_hydration__data__dataId": "HUMAN-1",
                |         "id": "FOO-3"
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
                |   humanById(ids: ["HUMAN-0", "HUMAN-1"]) {
                |     __typename
                |     id
                |     batch_hydration__data__id: id
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "humanById": [
                |       {
                |         "__typename": "Human",
                |         "id": "HUMAN-0",
                |         "batch_hydration__data__id": "HUMAN-0",
                |         "name": "Fanny Longbottom"
                |       },
                |       {
                |         "__typename": "Human",
                |         "id": "HUMAN-1",
                |         "batch_hydration__data__id": "HUMAN-1",
                |         "name": "John Doe"
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
                |   petById(ids: ["PET-0", "PET-1"]) {
                |     __typename
                |     breed
                |     id
                |     batch_hydration__data__id: id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "petById": [
                |       null,
                |       {
                |         "__typename": "Pet",
                |         "breed": "Labrador",
                |         "id": "PET-1",
                |         "batch_hydration__data__id": "PET-1"
                |       }
                |     ]
                |   },
                |   "errors": [
                |     {
                |       "message": "invalid id PET-0",
                |       "locations": [],
                |       "extensions": {
                |         "classification": "DataFetchingException"
                |       }
                |     }
                |   ]
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
     *         "__typename": "Foo",
     *         "id": "FOO-0",
     *         "data": null
     *       },
     *       {
     *         "__typename": "Foo",
     *         "id": "FOO-1",
     *         "data": {
     *           "__typename": "Human",
     *           "id": "HUMAN-0",
     *           "name": "Fanny Longbottom"
     *         }
     *       },
     *       {
     *         "__typename": "Foo",
     *         "id": "FOO-2",
     *         "data": {
     *           "__typename": "Pet",
     *           "breed": "Labrador",
     *           "id": "PET-1"
     *         }
     *       },
     *       {
     *         "__typename": "Foo",
     *         "id": "FOO-3",
     *         "data": {
     *           "__typename": "Human",
     *           "id": "HUMAN-1",
     *           "name": "John Doe"
     *         }
     *       }
     *     ]
     *   },
     *   "errors": [
     *     {
     *       "message": "invalid id PET-0",
     *       "locations": [],
     *       "extensions": {
     *         "classification": "DataFetchingException"
     *       }
     *     }
     *   ]
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "foo": [
            |       {
            |         "__typename": "Foo",
            |         "id": "FOO-0",
            |         "data": null
            |       },
            |       {
            |         "__typename": "Foo",
            |         "id": "FOO-1",
            |         "data": {
            |           "__typename": "Human",
            |           "id": "HUMAN-0",
            |           "name": "Fanny Longbottom"
            |         }
            |       },
            |       {
            |         "__typename": "Foo",
            |         "id": "FOO-2",
            |         "data": {
            |           "__typename": "Pet",
            |           "breed": "Labrador",
            |           "id": "PET-1"
            |         }
            |       },
            |       {
            |         "__typename": "Foo",
            |         "id": "FOO-3",
            |         "data": {
            |           "__typename": "Human",
            |           "id": "HUMAN-1",
            |           "name": "John Doe"
            |         }
            |       }
            |     ]
            |   },
            |   "errors": [
            |     {
            |       "message": "invalid id PET-0",
            |       "locations": [],
            |       "extensions": {
            |         "classification": "DataFetchingException"
            |       }
            |     }
            |   ]
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
