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
    graphql.nadel.tests.next.update<HydrationIdentifiedByRenamedFieldTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class HydrationIdentifiedByRenamedFieldTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "myService",
                query = """
                | {
                |   me {
                |     batch_hydration__friends__friendIds: friendIds
                |     __typename__batch_hydration__friends: __typename
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "me": {
                |       "batch_hydration__friends__friendIds": [
                |         "i",
                |         "2i"
                |       ],
                |       "__typename__batch_hydration__friends": "Me"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "myService",
                query = """
                | {
                |   users(ids: ["i", "2i"]) {
                |     name
                |     rename__batch_hydration__friends__id__canonicalAccountId: canonicalAccountId
                |     __typename__rename__batch_hydration__friends__id: __typename
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "users": [
                |       {
                |         "name": "Imagine",
                |         "rename__batch_hydration__friends__id__canonicalAccountId": "i",
                |         "__typename__rename__batch_hydration__friends__id": "User"
                |       },
                |       {
                |         "name": "Imagine 2 friends",
                |         "rename__batch_hydration__friends__id__canonicalAccountId": "2i",
                |         "__typename__rename__batch_hydration__friends__id": "User"
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
     *     "me": {
     *       "friends": [
     *         {
     *           "name": "Imagine"
     *         },
     *         {
     *           "name": "Imagine 2 friends"
     *         }
     *       ]
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "me": {
            |       "friends": [
            |         {
            |           "name": "Imagine"
            |         },
            |         {
            |           "name": "Imagine 2 friends"
            |         }
            |       ]
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
