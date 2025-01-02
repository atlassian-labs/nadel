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
    graphql.nadel.tests.next.update<`nested list hydration under a renamed top level field`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `nested list hydration under a renamed top level field snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Foo",
                query = """
                | {
                |   connection(id: "ID") {
                |     __typename__hydration__nodes: __typename
                |     hydration__nodes__edges: edges {
                |       node
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "connection": {
                |       "hydration__nodes__edges": [
                |         {
                |           "node": "1"
                |         }
                |       ],
                |       "__typename__hydration__nodes": "Connection"
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
                | {
                |   node(id: "1") {
                |     __typename__hydration__space: __typename
                |     hydration__space__id: id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "node": {
                |       "hydration__space__id": "1a",
                |       "__typename__hydration__space": "Node"
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
                | {
                |   rename__fooService__service: service {
                |     __typename__hydration__otherServices: __typename
                |     hydration__otherServices__id: id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "rename__fooService__service": {
                |       "hydration__otherServices__id": "ID",
                |       "__typename__hydration__otherServices": "Service"
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
                | {
                |   space(id: "1a") {
                |     id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "space": {
                |       "id": "apple"
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
     *     "fooService": {
     *       "otherServices": {
     *         "nodes": [
     *           {
     *             "space": {
     *               "id": "apple"
     *             }
     *           }
     *         ]
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
            |     "fooService": {
            |       "otherServices": {
            |         "nodes": [
            |           {
            |             "space": {
            |               "id": "apple"
            |             }
            |           }
            |         ]
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
