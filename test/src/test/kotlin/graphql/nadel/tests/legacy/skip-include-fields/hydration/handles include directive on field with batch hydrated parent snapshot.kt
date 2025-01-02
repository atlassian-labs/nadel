// @formatter:off
package graphql.nadel.tests.legacy.`skip-include-fields`.hydration

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`handles include directive on field with batch hydrated parent`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `handles include directive on field with batch hydrated parent snapshot` :
        TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "service",
                query = """
                | query {
                |   foos {
                |     __typename__batch_hydration__test: __typename
                |     batch_hydration__test__id: id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "foos": [
                |       {
                |         "__typename__batch_hydration__test": "Foo",
                |         "batch_hydration__test__id": "Foo-3"
                |       },
                |       {
                |         "__typename__batch_hydration__test": "Foo",
                |         "batch_hydration__test__id": "Foo-4"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "service",
                query = """
                | query {
                |   tests(ids: ["Foo-3", "Foo-4"]) {
                |     __typename__skip_include____skip: __typename
                |     batch_hydration__test__id: id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "tests": [
                |       {
                |         "__typename__skip_include____skip": "Foo",
                |         "batch_hydration__test__id": "Foo-4"
                |       },
                |       {
                |         "__typename__skip_include____skip": "Foo",
                |         "batch_hydration__test__id": "Foo-3"
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
     *         "test": {}
     *       },
     *       {
     *         "test": {}
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
            |         "test": {}
            |       },
            |       {
            |         "test": {}
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
