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
    graphql.nadel.tests.next.update<`nested renamed types`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `nested renamed types snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "service1",
                query = """
                | query {
                |   foo {
                |     __typename
                |     parent {
                |       __typename
                |       building {
                |         __typename
                |         id
                |       }
                |       id
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "foo": {
                |       "__typename": "Foo",
                |       "parent": {
                |         "__typename": "Foo",
                |         "id": "ParentFoo1",
                |         "building": {
                |           "__typename": "Building",
                |           "id": "Bar-1"
                |         }
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
     *       "__typename": "FooX",
     *       "parent": {
     *         "__typename": "FooX",
     *         "id": "ParentFoo1",
     *         "building": {
     *           "__typename": "Bar",
     *           "id": "Bar-1"
     *         }
     *       }
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
            |       "__typename": "FooX",
            |       "parent": {
            |         "__typename": "FooX",
            |         "id": "ParentFoo1",
            |         "building": {
            |           "__typename": "Bar",
            |           "id": "Bar-1"
            |         }
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
