// @formatter:off
package graphql.nadel.tests.legacy.renames

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`renamed field with normal field using same source`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `renamed field with normal field using same source snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Foo",
                query = """
                | query {
                |   foo {
                |     __typename__deep_rename__renamedField: __typename
                |     issue {
                |       fooDetail {
                |         name
                |       }
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
                |       "issue": {
                |         "fooDetail": {
                |           "name": "fooName"
                |         }
                |       },
                |       "__typename__deep_rename__renamedField": "Foo",
                |       "deep_rename__renamedField__issue": {
                |         "field": "field"
                |       }
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
     *         "fooDetail": {
     *           "name": "fooName"
     *         }
     *       },
     *       "renamedField": "field"
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
            |         "fooDetail": {
            |           "name": "fooName"
            |         }
            |       },
            |       "renamedField": "field"
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
