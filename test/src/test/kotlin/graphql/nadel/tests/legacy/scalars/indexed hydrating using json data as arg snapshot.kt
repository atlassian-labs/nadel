// @formatter:off
package graphql.nadel.tests.legacy.scalars

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`indexed hydrating using json data as arg`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `indexed hydrating using json data as arg snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Baz",
                query = """
                | query (${'$'}v0: [JSON!]!) {
                |   baz(data: ${'$'}v0) {
                |     id
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": [
                |     {
                |       "app-config": {
                |         "status": "deactivated",
                |         "bounce": true
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "baz": [
                |       {
                |         "id": "deactivated thing"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Baz",
                query = """
                | query (${'$'}v0: [JSON!]!) {
                |   baz(data: ${'$'}v0) {
                |     id
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": [
                |     {
                |       "id": "102",
                |       "appConfig": {
                |         "status": "active",
                |         "bounce": false
                |       }
                |     },
                |     {
                |       "ari": "ari:cloud:api-platform::thing/103",
                |       "config": {
                |         "status": "active",
                |         "bounce": true
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "baz": [
                |       {
                |         "id": "102"
                |       },
                |       {
                |         "id": "active bounce 103 thing"
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
                | query (${'$'}v0: JSON) {
                |   foo(input: ${'$'}v0) {
                |     __typename__batch_hydration__foo: __typename
                |     batch_hydration__foo__baz: baz
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": {
                |     "something": true,
                |     "answer": "42"
                |   }
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "foo": [
                |       {
                |         "batch_hydration__foo__baz": {
                |           "id": "102",
                |           "appConfig": {
                |             "status": "active",
                |             "bounce": false
                |           }
                |         },
                |         "__typename__batch_hydration__foo": "Foo"
                |       },
                |       {
                |         "batch_hydration__foo__baz": {
                |           "ari": "ari:cloud:api-platform::thing/103",
                |           "config": {
                |             "status": "active",
                |             "bounce": true
                |           }
                |         },
                |         "__typename__batch_hydration__foo": "Foo"
                |       },
                |       {
                |         "batch_hydration__foo__baz": {
                |           "app-config": {
                |             "status": "deactivated",
                |             "bounce": true
                |           }
                |         },
                |         "__typename__batch_hydration__foo": "Foo"
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
     *         "foo": {
     *           "id": "102"
     *         }
     *       },
     *       {
     *         "foo": {
     *           "id": "active bounce 103 thing"
     *         }
     *       },
     *       {
     *         "foo": {
     *           "id": "deactivated thing"
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
            |         "foo": {
            |           "id": "102"
            |         }
            |       },
            |       {
            |         "foo": {
            |           "id": "active bounce 103 thing"
            |         }
            |       },
            |       {
            |         "foo": {
            |           "id": "deactivated thing"
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
