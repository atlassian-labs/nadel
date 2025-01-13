// @formatter:off
package graphql.nadel.tests.next.fixtures.hydration

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<PolymorphicHydrationCommonInterfaceMigrationTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class PolymorphicHydrationCommonInterfaceMigrationTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "monolith",
                query = """
                | {
                |   oldObjects(ids: ["ari:cloud:owner::type/1"]) {
                |     __typename
                |     id
                |     batch_hydration__object__id: id
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "oldObjects": [
                |       {
                |         "__typename": "OldObject",
                |         "name": "Old object",
                |         "id": "ari:cloud:owner::type/1",
                |         "batch_hydration__object__id": "ari:cloud:owner::type/1"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "monolith",
                query = """
                | {
                |   reference {
                |     __typename__batch_hydration__object: __typename
                |     batch_hydration__object__objectId: objectId
                |     batch_hydration__object__objectId: objectId
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "reference": {
                |       "batch_hydration__object__objectId": "ari:cloud:owner::type/1",
                |       "__typename__batch_hydration__object": "Reference"
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
     *     "reference": {
     *       "object": {
     *         "__typename": "OldObject",
     *         "name": "Old object",
     *         "id": "ari:cloud:owner::type/1"
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
            |     "reference": {
            |       "object": {
            |         "__typename": "OldObject",
            |         "name": "Old object",
            |         "id": "ari:cloud:owner::type/1"
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
