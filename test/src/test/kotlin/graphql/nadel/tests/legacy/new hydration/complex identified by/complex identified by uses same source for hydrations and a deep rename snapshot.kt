// @formatter:off
package graphql.nadel.tests.legacy.`new hydration`.`complex identified by`

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`complex identified by uses same source for hydrations and a deep rename`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `complex identified by uses same source for hydrations and a deep rename snapshot` :
        TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Foo",
                query = """
                | query (${'$'}v0: [ID]) {
                |   details(detailIds: ${'$'}v0) {
                |     batch_hydration__detail__detailId: detailId
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": [
                |     "Foo-1",
                |     "Foo-2"
                |   ]
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "details": [
                |       {
                |         "name": "apple",
                |         "batch_hydration__detail__detailId": "Foo-1"
                |       },
                |       {
                |         "name": "Foo 2 Electric Boogaloo",
                |         "batch_hydration__detail__detailId": "Foo-2"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Foo",
                query = """
                | query (${'$'}v0: [ID]) {
                |   details(detailIds: ${'$'}v0) {
                |     batch_hydration__detail__detailId: detailId
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": [
                |     "Foo-3"
                |   ]
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "details": [
                |       {
                |         "name": "Three Apples",
                |         "batch_hydration__detail__detailId": "Foo-3"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Foo",
                query = """
                | query (${'$'}v0: [ID]) {
                |   issues(issueIds: ${'$'}v0) {
                |     field
                |     batch_hydration__issue__issueId: issueId
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": [
                |     "Foo-1",
                |     "Foo-2"
                |   ]
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "issues": [
                |       {
                |         "field": "field_name",
                |         "batch_hydration__issue__issueId": "Foo-1"
                |       },
                |       {
                |         "field": "field_name-2",
                |         "batch_hydration__issue__issueId": "Foo-2"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Foo",
                query = """
                | query (${'$'}v0: [ID]) {
                |   issues(issueIds: ${'$'}v0) {
                |     field
                |     batch_hydration__issue__issueId: issueId
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": [
                |     "Foo-3"
                |   ]
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "issues": [
                |       {
                |         "field": "field-3",
                |         "batch_hydration__issue__issueId": "Foo-3"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Foo",
                query = """
                | {
                |   foos {
                |     __typename__deep_rename__renamedField: __typename
                |     __typename__batch_hydration__issue: __typename
                |     __typename__batch_hydration__detail: __typename
                |     batch_hydration__issue__fooId: fooId
                |     batch_hydration__detail__fooId: fooId
                |     deep_rename__renamedField__issue: issue {
                |       field
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "foos": [
                |       {
                |         "deep_rename__renamedField__issue": {
                |           "field": "hmm-1"
                |         },
                |         "__typename__deep_rename__renamedField": "Foo",
                |         "batch_hydration__issue__fooId": "Foo-1",
                |         "__typename__batch_hydration__issue": "Foo",
                |         "batch_hydration__detail__fooId": "Foo-1",
                |         "__typename__batch_hydration__detail": "Foo"
                |       },
                |       {
                |         "deep_rename__renamedField__issue": {
                |           "field": "hmm-2"
                |         },
                |         "__typename__deep_rename__renamedField": "Foo",
                |         "batch_hydration__issue__fooId": "Foo-2",
                |         "__typename__batch_hydration__issue": "Foo",
                |         "batch_hydration__detail__fooId": "Foo-2",
                |         "__typename__batch_hydration__detail": "Foo"
                |       },
                |       {
                |         "deep_rename__renamedField__issue": {
                |           "field": "hmm-3"
                |         },
                |         "__typename__deep_rename__renamedField": "Foo",
                |         "batch_hydration__issue__fooId": "Foo-3",
                |         "__typename__batch_hydration__issue": "Foo",
                |         "batch_hydration__detail__fooId": "Foo-3",
                |         "__typename__batch_hydration__detail": "Foo"
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
     *     "foos": [
     *       {
     *         "detail": {
     *           "name": "apple"
     *         },
     *         "issue": {
     *           "field": "field_name"
     *         },
     *         "renamedField": "hmm-1"
     *       },
     *       {
     *         "detail": {
     *           "name": "Foo 2 Electric Boogaloo"
     *         },
     *         "issue": {
     *           "field": "field_name-2"
     *         },
     *         "renamedField": "hmm-2"
     *       },
     *       {
     *         "detail": {
     *           "name": "Three Apples"
     *         },
     *         "issue": {
     *           "field": "field-3"
     *         },
     *         "renamedField": "hmm-3"
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
            |     "foos": [
            |       {
            |         "detail": {
            |           "name": "apple"
            |         },
            |         "issue": {
            |           "field": "field_name"
            |         },
            |         "renamedField": "hmm-1"
            |       },
            |       {
            |         "detail": {
            |           "name": "Foo 2 Electric Boogaloo"
            |         },
            |         "issue": {
            |           "field": "field_name-2"
            |         },
            |         "renamedField": "hmm-2"
            |       },
            |       {
            |         "detail": {
            |           "name": "Three Apples"
            |         },
            |         "issue": {
            |           "field": "field-3"
            |         },
            |         "renamedField": "hmm-3"
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
