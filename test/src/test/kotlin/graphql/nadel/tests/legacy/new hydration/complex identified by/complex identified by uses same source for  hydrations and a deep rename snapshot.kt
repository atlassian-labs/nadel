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
    graphql.nadel.tests.next.update<`complex identified by uses same source for  hydrations and a deep rename`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `complex identified by uses same source for  hydrations and a deep rename snapshot` :
        TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Foo",
                query = """
                | query {
                |   details(detailIds: ["Foo-1", "Foo-2"]) {
                |     batch_hydration__detail__detailId: detailId
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "details": [
                |       {
                |         "batch_hydration__detail__detailId": "Foo-2",
                |         "name": "Foo 2 Electric Boogaloo"
                |       },
                |       {
                |         "batch_hydration__detail__detailId": "Foo-1",
                |         "name": "apple"
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
                | query {
                |   details(detailIds: ["Foo-4"]) {
                |     batch_hydration__detail__detailId: detailId
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "details": [
                |       {
                |         "batch_hydration__detail__detailId": "Foo-4",
                |         "name": "Four Apples"
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
                | query {
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
                |         "__typename__deep_rename__renamedField": "Foo",
                |         "__typename__batch_hydration__issue": "Foo",
                |         "__typename__batch_hydration__detail": "Foo",
                |         "batch_hydration__issue__fooId": "Foo-1",
                |         "batch_hydration__detail__fooId": "Foo-1",
                |         "deep_rename__renamedField__issue": {
                |           "field": "hmm-1"
                |         }
                |       },
                |       {
                |         "__typename__deep_rename__renamedField": "Foo",
                |         "__typename__batch_hydration__issue": "Foo",
                |         "__typename__batch_hydration__detail": "Foo",
                |         "batch_hydration__issue__fooId": "Foo-2",
                |         "batch_hydration__detail__fooId": "Foo-2",
                |         "deep_rename__renamedField__issue": {
                |           "field": "hmm-2"
                |         }
                |       },
                |       {
                |         "__typename__deep_rename__renamedField": "Foo",
                |         "__typename__batch_hydration__issue": "Foo",
                |         "__typename__batch_hydration__detail": "Foo",
                |         "batch_hydration__issue__fooId": "Foo-3",
                |         "batch_hydration__detail__fooId": "Foo-4",
                |         "deep_rename__renamedField__issue": {
                |           "field": "hmm-3"
                |         }
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
                | query {
                |   issues(issueIds: ["Foo-1", "Foo-2"]) {
                |     batch_hydration__issue__issueId: issueId
                |     field
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issues": [
                |       {
                |         "batch_hydration__issue__issueId": "Foo-1",
                |         "field": "field_name"
                |       },
                |       {
                |         "batch_hydration__issue__issueId": "Foo-2",
                |         "field": "field_name-2"
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
                | query {
                |   issues(issueIds: ["Foo-3"]) {
                |     batch_hydration__issue__issueId: issueId
                |     field
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issues": [
                |       {
                |         "batch_hydration__issue__issueId": "Foo-3",
                |         "field": "field-3"
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
     *         "renamedField": "hmm-1",
     *         "issue": {
     *           "field": "field_name"
     *         },
     *         "detail": {
     *           "name": "apple"
     *         }
     *       },
     *       {
     *         "renamedField": "hmm-2",
     *         "issue": {
     *           "field": "field_name-2"
     *         },
     *         "detail": {
     *           "name": "Foo 2 Electric Boogaloo"
     *         }
     *       },
     *       {
     *         "renamedField": "hmm-3",
     *         "issue": {
     *           "field": "field-3"
     *         },
     *         "detail": {
     *           "name": "Four Apples"
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
            |     "foos": [
            |       {
            |         "renamedField": "hmm-1",
            |         "issue": {
            |           "field": "field_name"
            |         },
            |         "detail": {
            |           "name": "apple"
            |         }
            |       },
            |       {
            |         "renamedField": "hmm-2",
            |         "issue": {
            |           "field": "field_name-2"
            |         },
            |         "detail": {
            |           "name": "Foo 2 Electric Boogaloo"
            |         }
            |       },
            |       {
            |         "renamedField": "hmm-3",
            |         "issue": {
            |           "field": "field-3"
            |         },
            |         "detail": {
            |           "name": "Four Apples"
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
