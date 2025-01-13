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
    graphql.nadel.tests.next.update<PolymorphicHydrationMigrationTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class PolymorphicHydrationMigrationTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "monolith",
                query = """
                | {
                |   both_query: reference {
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
                |     "both_query": {
                |       "batch_hydration__object__objectId": "ari:cloud:owner::type/1",
                |       "__typename__batch_hydration__object": "Reference"
                |     }
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
                |   migrate_off: reference {
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
                |     "migrate_off": {
                |       "batch_hydration__object__objectId": "ari:cloud:owner::type/1",
                |       "__typename__batch_hydration__object": "Reference"
                |     }
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
                |   migrate_on: reference {
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
                |     "migrate_on": {
                |       "batch_hydration__object__objectId": "ari:cloud:owner::type/1",
                |       "__typename__batch_hydration__object": "Reference"
                |     }
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
                |   newObjects(ids: ["ari:cloud:owner::type/1"]) {
                |     __typename
                |     batch_hydration__object__id: id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "newObjects": [
                |       {
                |         "__typename": "NewObject",
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
                |   newObjects(ids: ["ari:cloud:owner::type/1"]) {
                |     __typename
                |     id
                |     batch_hydration__object__id: id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "newObjects": [
                |       {
                |         "__typename": "NewObject",
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
                |   newObjects(ids: ["ari:cloud:owner::type/1"]) {
                |     __typename
                |     id
                |     batch_hydration__object__id: id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "newObjects": [
                |       {
                |         "__typename": "NewObject",
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
                |   newObjects(ids: ["ari:cloud:owner::type/1"]) {
                |     __typename
                |     id
                |     batch_hydration__object__id: id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "newObjects": [
                |       {
                |         "__typename": "NewObject",
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
                |   new_query: reference {
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
                |     "new_query": {
                |       "batch_hydration__object__objectId": "ari:cloud:owner::type/1",
                |       "__typename__batch_hydration__object": "Reference"
                |     }
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
                |   nothing: reference {
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
                |     "nothing": {
                |       "batch_hydration__object__objectId": "ari:cloud:owner::type/1",
                |       "__typename__batch_hydration__object": "Reference"
                |     }
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
                |   oldObjects(ids: ["ari:cloud:owner::type/1"]) {
                |     __typename
                |     id
                |     batch_hydration__object__id: id
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
                |   oldObjects(ids: ["ari:cloud:owner::type/1"]) {
                |     __typename
                |     id
                |     batch_hydration__object__id: id
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
                |   old_query: reference {
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
                |     "old_query": {
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
     *     "nothing": {
     *       "object": {
     *         "__typename": "NewObject"
     *       }
     *     },
     *     "old_query": {
     *       "object": {
     *         "__typename": "OldObject",
     *         "id": "ari:cloud:owner::type/1"
     *       }
     *     },
     *     "new_query": {
     *       "object": {
     *         "__typename": "NewObject",
     *         "id": "ari:cloud:owner::type/1"
     *       }
     *     },
     *     "both_query": {
     *       "object": {
     *         "__typename": "NewObject",
     *         "id": "ari:cloud:owner::type/1"
     *       }
     *     },
     *     "migrate_off": {
     *       "object": {
     *         "__typename": "OldObject",
     *         "id": "ari:cloud:owner::type/1"
     *       }
     *     },
     *     "migrate_on": {
     *       "object": {
     *         "__typename": "NewObject",
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
            |     "nothing": {
            |       "object": {
            |         "__typename": "NewObject"
            |       }
            |     },
            |     "old_query": {
            |       "object": {
            |         "__typename": "OldObject",
            |         "id": "ari:cloud:owner::type/1"
            |       }
            |     },
            |     "new_query": {
            |       "object": {
            |         "__typename": "NewObject",
            |         "id": "ari:cloud:owner::type/1"
            |       }
            |     },
            |     "both_query": {
            |       "object": {
            |         "__typename": "NewObject",
            |         "id": "ari:cloud:owner::type/1"
            |       }
            |     },
            |     "migrate_off": {
            |       "object": {
            |         "__typename": "OldObject",
            |         "id": "ari:cloud:owner::type/1"
            |       }
            |     },
            |     "migrate_on": {
            |       "object": {
            |         "__typename": "NewObject",
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
