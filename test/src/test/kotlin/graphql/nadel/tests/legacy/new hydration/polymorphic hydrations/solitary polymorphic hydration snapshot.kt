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
    graphql.nadel.tests.next.update<`solitary polymorphic hydration`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `solitary polymorphic hydration snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "foo",
                query = """
                | query {
                |   foo {
                |     __typename
                |     __typename__hydration__data: __typename
                |     hydration__data__dataId: dataId
                |     hydration__data__dataId: dataId
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
                |         "__typename__hydration__data": "Foo",
                |         "hydration__data__dataId": "PET-0",
                |         "id": "FOO-0"
                |       },
                |       {
                |         "__typename": "Foo",
                |         "__typename__hydration__data": "Foo",
                |         "hydration__data__dataId": "HUMAN-0",
                |         "id": "FOO-1"
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
                | query {
                |   humanById(id: "HUMAN-0") {
                |     __typename
                |     __typename__type_filter__id: __typename
                |     __typename__type_filter__breed: __typename
                |     id
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "humanById": {
                |       "__typename": "Human",
                |       "__typename__type_filter__id": "Human",
                |       "__typename__type_filter__breed": "Human",
                |       "id": "HUMAN-0",
                |       "name": "Fanny Longbottom"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "pets",
                query = """
                | query {
                |   petById(id: "PET-0") {
                |     __typename
                |     __typename__type_filter__id: __typename
                |     __typename__type_filter__name: __typename
                |     breed
                |     id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "petById": {
                |       "__typename": "Pet",
                |       "__typename__type_filter__id": "Pet",
                |       "__typename__type_filter__name": "Pet",
                |       "id": "PET-0",
                |       "breed": "Akita"
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
     *     "foo": [
     *       {
     *         "__typename": "Foo",
     *         "id": "FOO-0",
     *         "data": {
     *           "__typename": "Pet",
     *           "id": "PET-0",
     *           "breed": "Akita"
     *         }
     *       },
     *       {
     *         "__typename": "Foo",
     *         "id": "FOO-1",
     *         "data": {
     *           "__typename": "Human",
     *           "id": "HUMAN-0",
     *           "name": "Fanny Longbottom"
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
            |     "foo": [
            |       {
            |         "__typename": "Foo",
            |         "id": "FOO-0",
            |         "data": {
            |           "__typename": "Pet",
            |           "id": "PET-0",
            |           "breed": "Akita"
            |         }
            |       },
            |       {
            |         "__typename": "Foo",
            |         "id": "FOO-1",
            |         "data": {
            |           "__typename": "Human",
            |           "id": "HUMAN-0",
            |           "name": "Fanny Longbottom"
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
