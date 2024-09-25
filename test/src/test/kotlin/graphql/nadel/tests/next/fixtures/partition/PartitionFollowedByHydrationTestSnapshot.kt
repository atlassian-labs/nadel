// @formatter:off
package graphql.nadel.tests.next.fixtures.partition

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<PartitionFollowedByHydrationTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class PartitionFollowedByHydrationTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "things_service",
                query = """
                | query getPartitionedThings {
                |   things(ids: ["thing-1:partition-A", "thing-3:partition-A"]) {
                |     id
                |     name
                |     batch_hydration__owner__ownerId: ownerId
                |     __typename__batch_hydration__owner: __typename
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "things": [
                |       {
                |         "id": "thing-1",
                |         "name": "THING-1",
                |         "batch_hydration__owner__ownerId": "owner-thing-1",
                |         "__typename__batch_hydration__owner": "Thing"
                |       },
                |       {
                |         "id": "thing-3",
                |         "name": "THING-3",
                |         "batch_hydration__owner__ownerId": "owner-thing-3",
                |         "__typename__batch_hydration__owner": "Thing"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "things_service",
                query = """
                | query getPartitionedThings {
                |   things(ids: ["thing-2:partition-B", "thing-4:partition-B"]) {
                |     id
                |     name
                |     batch_hydration__owner__ownerId: ownerId
                |     __typename__batch_hydration__owner: __typename
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "things": [
                |       {
                |         "id": "thing-2",
                |         "name": "THING-2",
                |         "batch_hydration__owner__ownerId": "owner-thing-2",
                |         "__typename__batch_hydration__owner": "Thing"
                |       },
                |       {
                |         "id": "thing-4",
                |         "name": "THING-4",
                |         "batch_hydration__owner__ownerId": "owner-thing-4",
                |         "__typename__batch_hydration__owner": "Thing"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "things_service",
                query = """
                | query getPartitionedThings {
                |   things(ids: ["thing-5:partition-C", "thing-7:partition-C"]) {
                |     id
                |     name
                |     batch_hydration__owner__ownerId: ownerId
                |     __typename__batch_hydration__owner: __typename
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "things": [
                |       {
                |         "id": "thing-5",
                |         "name": "THING-5",
                |         "batch_hydration__owner__ownerId": "owner-thing-5",
                |         "__typename__batch_hydration__owner": "Thing"
                |       },
                |       {
                |         "id": "thing-7",
                |         "name": "THING-7",
                |         "batch_hydration__owner__ownerId": "owner-thing-7",
                |         "__typename__batch_hydration__owner": "Thing"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "things_service",
                query = """
                | query getPartitionedThings {
                |   things(ids: ["thing-6:partition-D", "thing-8:partition-D"]) {
                |     id
                |     name
                |     batch_hydration__owner__ownerId: ownerId
                |     __typename__batch_hydration__owner: __typename
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "things": [
                |       {
                |         "id": "thing-6",
                |         "name": "THING-6",
                |         "batch_hydration__owner__ownerId": "owner-thing-6",
                |         "__typename__batch_hydration__owner": "Thing"
                |       },
                |       {
                |         "id": "thing-8",
                |         "name": "THING-8",
                |         "batch_hydration__owner__ownerId": "owner-thing-8",
                |         "__typename__batch_hydration__owner": "Thing"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "users_service",
                query = """
                | query getPartitionedThings {
                |   users(ids: ["owner-thing-1", "owner-thing-3"]) {
                |     id
                |     name
                |     batch_hydration__owner__id: id
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "users": [
                |       {
                |         "id": "owner-thing-1",
                |         "name": "OWNER-THING-1",
                |         "batch_hydration__owner__id": "owner-thing-1"
                |       },
                |       {
                |         "id": "owner-thing-3",
                |         "name": "OWNER-THING-3",
                |         "batch_hydration__owner__id": "owner-thing-3"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "users_service",
                query = """
                | query getPartitionedThings {
                |   users(ids: ["owner-thing-2", "owner-thing-4"]) {
                |     id
                |     name
                |     batch_hydration__owner__id: id
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "users": [
                |       {
                |         "id": "owner-thing-2",
                |         "name": "OWNER-THING-2",
                |         "batch_hydration__owner__id": "owner-thing-2"
                |       },
                |       {
                |         "id": "owner-thing-4",
                |         "name": "OWNER-THING-4",
                |         "batch_hydration__owner__id": "owner-thing-4"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "users_service",
                query = """
                | query getPartitionedThings {
                |   users(ids: ["owner-thing-5", "owner-thing-7"]) {
                |     id
                |     name
                |     batch_hydration__owner__id: id
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "users": [
                |       {
                |         "id": "owner-thing-5",
                |         "name": "OWNER-THING-5",
                |         "batch_hydration__owner__id": "owner-thing-5"
                |       },
                |       {
                |         "id": "owner-thing-7",
                |         "name": "OWNER-THING-7",
                |         "batch_hydration__owner__id": "owner-thing-7"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "users_service",
                query = """
                | query getPartitionedThings {
                |   users(ids: ["owner-thing-6", "owner-thing-8"]) {
                |     id
                |     name
                |     batch_hydration__owner__id: id
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "users": [
                |       {
                |         "id": "owner-thing-6",
                |         "name": "OWNER-THING-6",
                |         "batch_hydration__owner__id": "owner-thing-6"
                |       },
                |       {
                |         "id": "owner-thing-8",
                |         "name": "OWNER-THING-8",
                |         "batch_hydration__owner__id": "owner-thing-8"
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
     *     "things": [
     *       {
     *         "id": "thing-1",
     *         "name": "THING-1",
     *         "owner": {
     *           "id": "owner-thing-1",
     *           "name": "OWNER-THING-1"
     *         }
     *       },
     *       {
     *         "id": "thing-3",
     *         "name": "THING-3",
     *         "owner": {
     *           "id": "owner-thing-3",
     *           "name": "OWNER-THING-3"
     *         }
     *       },
     *       {
     *         "id": "thing-2",
     *         "name": "THING-2",
     *         "owner": {
     *           "id": "owner-thing-2",
     *           "name": "OWNER-THING-2"
     *         }
     *       },
     *       {
     *         "id": "thing-4",
     *         "name": "THING-4",
     *         "owner": {
     *           "id": "owner-thing-4",
     *           "name": "OWNER-THING-4"
     *         }
     *       },
     *       {
     *         "id": "thing-5",
     *         "name": "THING-5",
     *         "owner": {
     *           "id": "owner-thing-5",
     *           "name": "OWNER-THING-5"
     *         }
     *       },
     *       {
     *         "id": "thing-7",
     *         "name": "THING-7",
     *         "owner": {
     *           "id": "owner-thing-7",
     *           "name": "OWNER-THING-7"
     *         }
     *       },
     *       {
     *         "id": "thing-6",
     *         "name": "THING-6",
     *         "owner": {
     *           "id": "owner-thing-6",
     *           "name": "OWNER-THING-6"
     *         }
     *       },
     *       {
     *         "id": "thing-8",
     *         "name": "THING-8",
     *         "owner": {
     *           "id": "owner-thing-8",
     *           "name": "OWNER-THING-8"
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
            |     "things": [
            |       {
            |         "id": "thing-1",
            |         "name": "THING-1",
            |         "owner": {
            |           "id": "owner-thing-1",
            |           "name": "OWNER-THING-1"
            |         }
            |       },
            |       {
            |         "id": "thing-3",
            |         "name": "THING-3",
            |         "owner": {
            |           "id": "owner-thing-3",
            |           "name": "OWNER-THING-3"
            |         }
            |       },
            |       {
            |         "id": "thing-2",
            |         "name": "THING-2",
            |         "owner": {
            |           "id": "owner-thing-2",
            |           "name": "OWNER-THING-2"
            |         }
            |       },
            |       {
            |         "id": "thing-4",
            |         "name": "THING-4",
            |         "owner": {
            |           "id": "owner-thing-4",
            |           "name": "OWNER-THING-4"
            |         }
            |       },
            |       {
            |         "id": "thing-5",
            |         "name": "THING-5",
            |         "owner": {
            |           "id": "owner-thing-5",
            |           "name": "OWNER-THING-5"
            |         }
            |       },
            |       {
            |         "id": "thing-7",
            |         "name": "THING-7",
            |         "owner": {
            |           "id": "owner-thing-7",
            |           "name": "OWNER-THING-7"
            |         }
            |       },
            |       {
            |         "id": "thing-6",
            |         "name": "THING-6",
            |         "owner": {
            |           "id": "owner-thing-6",
            |           "name": "OWNER-THING-6"
            |         }
            |       },
            |       {
            |         "id": "thing-8",
            |         "name": "THING-8",
            |         "owner": {
            |           "id": "owner-thing-8",
            |           "name": "OWNER-THING-8"
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
