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
    graphql.nadel.tests.next.update<`same source for  nested hydrations and a rename`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `same source for  nested hydrations and a rename snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Foo",
                query = """
                | query {
                |   detail(detailId: "ID") {
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "detail": {
                |       "name": "apple"
                |     }
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
                |   foo {
                |     __typename__hydration__issue: __typename
                |     __typename__hydration__detail: __typename
                |     __typename__deep_rename__renamedField: __typename
                |     hydration__issue__issue: issue {
                |       fooId
                |     }
                |     hydration__detail__issue: issue {
                |       fooId
                |     }
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
                |     "foo": {
                |       "__typename__deep_rename__renamedField": "Foo",
                |       "hydration__issue__issue": {
                |         "fooId": "ID"
                |       },
                |       "hydration__detail__issue": {
                |         "fooId": "ID"
                |       },
                |       "__typename__hydration__issue": "Foo",
                |       "__typename__hydration__detail": "Foo",
                |       "deep_rename__renamedField__issue": {
                |         "field": "field1"
                |       }
                |     }
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
                |   issue(issueId: "ID") {
                |     field
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issue": {
                |       "field": "field_name"
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
     *     "foo": {
     *       "issue": {
     *         "field": "field_name"
     *       },
     *       "detail": {
     *         "name": "apple"
     *       },
     *       "renamedField": "field1"
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "foo": {
            |       "issue": {
            |         "field": "field_name"
            |       },
            |       "detail": {
            |         "name": "apple"
            |       },
            |       "renamedField": "field1"
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
