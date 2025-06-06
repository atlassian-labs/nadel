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
    graphql.nadel.tests.next.update<`batch polymorphic hydration with lots of renames`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `batch polymorphic hydration with lots of renames snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "bar",
                query = """
                | {
                |   humanById(ids: ["HUMAN-0", "HUMAN-1"]) {
                |     __typename
                |     __typename__rename__id: __typename
                |     batch_hydration__data__hiddenId: hiddenId
                |     rename__id__identifier: identifier
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
                |         "rename__id__identifier": "PERSON-0",
                |         "__typename__rename__id": "Human",
                |         "name": "Fanny Longbottom",
                |         "batch_hydration__data__hiddenId": "HUMAN-0"
                |       },
                |       {
                |         "__typename": "Human",
                |         "rename__id__identifier": "PERSON-1",
                |         "__typename__rename__id": "Human",
                |         "name": "John Doe",
                |         "batch_hydration__data__hiddenId": "HUMAN-1"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "bar",
                query = """
                | {
                |   petById(ids: ["PET-0", "PET-1"]) {
                |     __typename
                |     __typename__rename__id: __typename
                |     __typename__rename__breed: __typename
                |     batch_hydration__data__hiddenId: hiddenId
                |     rename__id__identifier: identifier
                |     rename__breed__kind: kind
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "petById": [
                |       {
                |         "__typename": "Pet",
                |         "rename__id__identifier": "ANIMAL-0",
                |         "__typename__rename__id": "Pet",
                |         "rename__breed__kind": "Akita",
                |         "__typename__rename__breed": "Pet",
                |         "batch_hydration__data__hiddenId": "PET-0"
                |       },
                |       {
                |         "__typename": "Pet",
                |         "rename__id__identifier": "ANIMAL-1",
                |         "__typename__rename__id": "Pet",
                |         "rename__breed__kind": "Labrador",
                |         "__typename__rename__breed": "Pet",
                |         "batch_hydration__data__hiddenId": "PET-1"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
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
                |         "id": "FOO-0",
                |         "batch_hydration__data__dataId": "PET-0",
                |         "__typename__batch_hydration__data": "Foo"
                |       },
                |       {
                |         "__typename": "Foo",
                |         "id": "FOO-1",
                |         "batch_hydration__data__dataId": "HUMAN-0",
                |         "__typename__batch_hydration__data": "Foo"
                |       },
                |       {
                |         "__typename": "Foo",
                |         "id": "FOO-2",
                |         "batch_hydration__data__dataId": "PET-1",
                |         "__typename__batch_hydration__data": "Foo"
                |       },
                |       {
                |         "__typename": "Foo",
                |         "id": "FOO-3",
                |         "batch_hydration__data__dataId": "HUMAN-1",
                |         "__typename__batch_hydration__data": "Foo"
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
     *         "__typename": "Foo",
     *         "id": "FOO-0",
     *         "data": {
     *           "__typename": "Animal",
     *           "id": "ANIMAL-0",
     *           "breed": "Akita"
     *         }
     *       },
     *       {
     *         "__typename": "Foo",
     *         "id": "FOO-1",
     *         "data": {
     *           "__typename": "Person",
     *           "name": "Fanny Longbottom",
     *           "id": "PERSON-0"
     *         }
     *       },
     *       {
     *         "__typename": "Foo",
     *         "id": "FOO-2",
     *         "data": {
     *           "__typename": "Animal",
     *           "id": "ANIMAL-1",
     *           "breed": "Labrador"
     *         }
     *       },
     *       {
     *         "__typename": "Foo",
     *         "id": "FOO-3",
     *         "data": {
     *           "__typename": "Person",
     *           "name": "John Doe",
     *           "id": "PERSON-1"
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
            |           "__typename": "Animal",
            |           "id": "ANIMAL-0",
            |           "breed": "Akita"
            |         }
            |       },
            |       {
            |         "__typename": "Foo",
            |         "id": "FOO-1",
            |         "data": {
            |           "__typename": "Person",
            |           "name": "Fanny Longbottom",
            |           "id": "PERSON-0"
            |         }
            |       },
            |       {
            |         "__typename": "Foo",
            |         "id": "FOO-2",
            |         "data": {
            |           "__typename": "Animal",
            |           "id": "ANIMAL-1",
            |           "breed": "Labrador"
            |         }
            |       },
            |       {
            |         "__typename": "Foo",
            |         "id": "FOO-3",
            |         "data": {
            |           "__typename": "Person",
            |           "name": "John Doe",
            |           "id": "PERSON-1"
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
