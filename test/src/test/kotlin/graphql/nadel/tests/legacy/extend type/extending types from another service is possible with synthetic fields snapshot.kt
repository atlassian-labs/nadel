// @formatter:off
package graphql.nadel.tests.legacy.`extend type`

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`extending types from another service is possible with synthetic fields`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `extending types from another service is possible with synthetic fields snapshot` :
        TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Service1",
                query = """
                | query {
                |   anotherRoot
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "anotherRoot": "anotherRoot"
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Service1",
                query = """
                | query {
                |   root {
                |     __typename__hydration__extension: __typename
                |     id
                |     hydration__extension__id: id
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "root": {
                |       "id": "rootId",
                |       "hydration__extension__id": "rootId",
                |       "__typename__hydration__extension": "Root",
                |       "name": "rootName"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Service2",
                query = """
                | query {
                |   lookUpQuery {
                |     lookup(id: "rootId") {
                |       id
                |       name
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "lookUpQuery": {
                |       "lookup": {
                |         "id": "rootId",
                |         "name": "extensionName"
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
     *     "root": {
     *       "id": "rootId",
     *       "extension": {
     *         "id": "rootId",
     *         "name": "extensionName"
     *       },
     *       "name": "rootName"
     *     },
     *     "anotherRoot": "anotherRoot"
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "root": {
            |       "id": "rootId",
            |       "extension": {
            |         "id": "rootId",
            |         "name": "extensionName"
            |       },
            |       "name": "rootName"
            |     },
            |     "anotherRoot": "anotherRoot"
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
