// @formatter:off
package graphql.nadel.tests.next.fixtures.hydration.conditional

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<ConditionalHydrationRegexTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class ConditionalHydrationRegexTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "identity",
                query = """
                | {
                |   userById(id: "ari:cloud:identity::user/1") {
                |     __typename
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "userById": {
                |       "__typename": "User"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "issues",
                query = """
                | {
                |   issueById(id: "ari:cloud:jira:TEST--67b8ae80-cf1a-4de7-b77c-3f8ff51977b3:issue/10000") {
                |     __typename
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "issueById": {
                |       "__typename": "Issue"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "search",
                query = """
                | {
                |   search {
                |     hydration__object__objectId: objectId
                |     hydration__object__objectId: objectId
                |     hydration__object__objectId: objectId
                |     hydration__object__objectId: objectId
                |     __typename__hydration__object: __typename
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "search": [
                |       {
                |         "hydration__object__objectId": "ari:cloud:jira:TEST--67b8ae80-cf1a-4de7-b77c-3f8ff51977b3:issue/10000",
                |         "__typename__hydration__object": "SearchResult"
                |       },
                |       {
                |         "hydration__object__objectId": "ari:cloud:identity::user/1",
                |         "__typename__hydration__object": "SearchResult"
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
     *     "search": [
     *       {
     *         "object": {
     *           "__typename": "Issue"
     *         }
     *       },
     *       {
     *         "object": {
     *           "__typename": "User"
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
            |     "search": [
            |       {
            |         "object": {
            |           "__typename": "Issue"
            |         }
            |       },
            |       {
            |         "object": {
            |           "__typename": "User"
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
