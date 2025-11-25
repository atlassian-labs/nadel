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
    graphql.nadel.tests.next.update<HydrationNonBatchedMultipleSourceArgumentsConditionalTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class HydrationNonBatchedMultipleSourceArgumentsConditionalTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "myService",
                query = """
                | {
                |   appUser(siteId: "site1", userId: "wow") {
                |     __typename
                |     ... on AppUser {
                |       appUserId: id
                |     }
                |     ... on QueryError {
                |       message
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "appUser": {
                |       "__typename": "QueryError",
                |       "message": "Could not find user wow on site site1"
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
                |   appUser(siteId: "site2", userId: "aoeu") {
                |     __typename
                |     ... on AppUser {
                |       appUserId: id
                |     }
                |     ... on QueryError {
                |       message
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "appUser": {
                |       "__typename": "QueryError",
                |       "message": "Found user aoeu but the user is not a AppUser"
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
                |   appUser(siteId: "site2", userId: "wow") {
                |     __typename
                |     ... on AppUser {
                |       appUserId: id
                |     }
                |     ... on QueryError {
                |       message
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "appUser": {
                |       "__typename": "QueryError",
                |       "message": "Could not find user wow on site site2"
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
                |   atlassianAccountUser(siteId: "site1", userId: "aoeu") {
                |     __typename
                |     ... on AtlassianAccountUser {
                |       atlassianAccountUserId: id
                |     }
                |     ... on QueryError {
                |       message
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "atlassianAccountUser": {
                |       "__typename": "AtlassianAccountUser",
                |       "atlassianAccountUserId": "aoeu"
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
                |   customerUser(siteId: "site2", userId: "asdf") {
                |     __typename
                |     ... on CustomerUser {
                |       customerUserId: id
                |     }
                |     ... on QueryError {
                |       message
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "customerUser": {
                |       "__typename": "CustomerUser",
                |       "customerUserId": "asdf"
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
                |   issues {
                |     __typename__hydration__user: __typename
                |     hydration__user__siteId: siteId
                |     hydration__user__siteId: siteId
                |     hydration__user__siteId: siteId
                |     hydration__user__userId: userId
                |     hydration__user__userId: userId
                |     hydration__user__userId: userId
                |     hydration__user__userType: userType
                |     hydration__user__userType: userType
                |     hydration__user__userType: userType
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issues": [
                |       {
                |         "hydration__user__siteId": "site1",
                |         "hydration__user__userId": "aoeu",
                |         "hydration__user__userType": "ATLASSIAN_ACCOUNT_USER",
                |         "__typename__hydration__user": "Issue"
                |       },
                |       {
                |         "hydration__user__siteId": "site2",
                |         "hydration__user__userId": "asdf",
                |         "hydration__user__userType": "CUSTOMER_USER",
                |         "__typename__hydration__user": "Issue"
                |       },
                |       {
                |         "hydration__user__siteId": "site1",
                |         "hydration__user__userId": "wow",
                |         "hydration__user__userType": "APP_USER",
                |         "__typename__hydration__user": "Issue"
                |       },
                |       {
                |         "hydration__user__siteId": "site2",
                |         "hydration__user__userId": "wow",
                |         "hydration__user__userType": "APP_USER",
                |         "__typename__hydration__user": "Issue"
                |       },
                |       {
                |         "hydration__user__siteId": "site2",
                |         "hydration__user__userId": "aoeu",
                |         "hydration__user__userType": "APP_USER",
                |         "__typename__hydration__user": "Issue"
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
     *         "user": {
     *           "__typename": "AtlassianAccountUser",
     *           "atlassianAccountUserId": "aoeu"
     *         }
     *       },
     *       {
     *         "user": {
     *           "__typename": "CustomerUser",
     *           "customerUserId": "asdf"
     *         }
     *       },
     *       {
     *         "user": {
     *           "__typename": "QueryError",
     *           "message": "Could not find user wow on site site1"
     *         }
     *       },
     *       {
     *         "user": {
     *           "__typename": "QueryError",
     *           "message": "Could not find user wow on site site2"
     *         }
     *       },
     *       {
     *         "user": {
     *           "__typename": "QueryError",
     *           "message": "Found user aoeu but the user is not a AppUser"
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
            |         "user": {
            |           "__typename": "AtlassianAccountUser",
            |           "atlassianAccountUserId": "aoeu"
            |         }
            |       },
            |       {
            |         "user": {
            |           "__typename": "CustomerUser",
            |           "customerUserId": "asdf"
            |         }
            |       },
            |       {
            |         "user": {
            |           "__typename": "QueryError",
            |           "message": "Could not find user wow on site site1"
            |         }
            |       },
            |       {
            |         "user": {
            |           "__typename": "QueryError",
            |           "message": "Could not find user wow on site site2"
            |         }
            |       },
            |       {
            |         "user": {
            |           "__typename": "QueryError",
            |           "message": "Found user aoeu but the user is not a AppUser"
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
