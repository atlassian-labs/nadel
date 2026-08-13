// @formatter:off
package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<BatchHydrationCoalescingMultiOriginFallbackTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class BatchHydrationCoalescingMultiOriginFallbackTestSnapshot : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * query {
     *   issues {
     *     first: user {
     *       __typename
     *     }
     *     second: user {
     *       __typename
     *     }
     *   }
     * }
     * ```
     *
     * Variables
     *
     * ```json
     * {}
     * ```
     */
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Identity",
                query = """
                | {
                |   issues {
                |     __typename__batch_hydration__first: __typename
                |     __typename__batch_hydration__second: __typename
                |     batch_hydration__first__userId: userId
                |     batch_hydration__second__userId: userId
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issues": [
                |       {
                |         "batch_hydration__first__userId": "ari:cloud:identity::user/customer",
                |         "__typename__batch_hydration__first": "Issue",
                |         "batch_hydration__second__userId": "ari:cloud:identity::user/customer",
                |         "__typename__batch_hydration__second": "Issue"
                |       },
                |       {
                |         "batch_hydration__first__userId": "ari:cloud:identity::user/account",
                |         "__typename__batch_hydration__first": "Issue",
                |         "batch_hydration__second__userId": "ari:cloud:identity::user/account",
                |         "__typename__batch_hydration__second": "Issue"
                |       },
                |       {
                |         "batch_hydration__first__userId": "ari:cloud:identity::user/app",
                |         "__typename__batch_hydration__first": "Issue",
                |         "batch_hydration__second__userId": "ari:cloud:identity::user/app",
                |         "__typename__batch_hydration__second": "Issue"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Identity",
                query = """
                | {
                |   usersByIds(ids: ["ari:cloud:identity::user/customer", "ari:cloud:identity::user/account", "ari:cloud:identity::user/app"]) {
                |     __typename
                |     batch_hydration__first__id: id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "usersByIds": [
                |       {
                |         "__typename": "CustomerUser",
                |         "batch_hydration__first__id": "ari:cloud:identity::user/customer"
                |       },
                |       {
                |         "__typename": "AtlassianAccountUser",
                |         "batch_hydration__first__id": "ari:cloud:identity::user/account"
                |       },
                |       {
                |         "__typename": "AppUser",
                |         "batch_hydration__first__id": "ari:cloud:identity::user/app"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Identity",
                query = """
                | {
                |   usersByIds(ids: ["ari:cloud:identity::user/customer", "ari:cloud:identity::user/account", "ari:cloud:identity::user/app"]) {
                |     __typename
                |     batch_hydration__second__id: id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "usersByIds": [
                |       {
                |         "__typename": "CustomerUser",
                |         "batch_hydration__second__id": "ari:cloud:identity::user/customer"
                |       },
                |       {
                |         "__typename": "AtlassianAccountUser",
                |         "batch_hydration__second__id": "ari:cloud:identity::user/account"
                |       },
                |       {
                |         "__typename": "AppUser",
                |         "batch_hydration__second__id": "ari:cloud:identity::user/app"
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
     *     "issues": [
     *       {
     *         "second": {
     *           "__typename": "CustomerUser"
     *         },
     *         "first": {
     *           "__typename": "CustomerUser"
     *         }
     *       },
     *       {
     *         "second": {
     *           "__typename": "AtlassianAccountUser"
     *         },
     *         "first": {
     *           "__typename": "AtlassianAccountUser"
     *         }
     *       },
     *       {
     *         "second": {
     *           "__typename": "AppUser"
     *         },
     *         "first": {
     *           "__typename": "AppUser"
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
            |     "issues": [
            |       {
            |         "second": {
            |           "__typename": "CustomerUser"
            |         },
            |         "first": {
            |           "__typename": "CustomerUser"
            |         }
            |       },
            |       {
            |         "second": {
            |           "__typename": "AtlassianAccountUser"
            |         },
            |         "first": {
            |           "__typename": "AtlassianAccountUser"
            |         }
            |       },
            |       {
            |         "second": {
            |           "__typename": "AppUser"
            |         },
            |         "first": {
            |           "__typename": "AppUser"
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
