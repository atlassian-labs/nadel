// @formatter:off
package graphql.nadel.tests.next.fixtures.hydration.defer

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<HydrationDeferInListTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class HydrationDeferInListTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "issues",
                query = """
                | query (${'$'}v0: String!) {
                |   issueByKey(key: ${'$'}v0) {
                |     __typename__hydration__assignee: __typename
                |     hydration__assignee__assigneeId: assigneeId
                |     key
                |     related {
                |       __typename__hydration__assignee: __typename
                |       hydration__assignee__assigneeId: assigneeId
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "GQLGW-2"
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "issueByKey": {
                |       "key": "GQLGW-2",
                |       "hydration__assignee__assigneeId": "ari:cloud:identity::user/2",
                |       "__typename__hydration__assignee": "Issue",
                |       "related": [
                |         {
                |           "hydration__assignee__assigneeId": "ari:cloud:identity::user/1",
                |           "__typename__hydration__assignee": "Issue"
                |         }
                |       ]
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "users",
                query = """
                | query (${'$'}v0: ID!) {
                |   userById(id: ${'$'}v0) {
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "ari:cloud:identity::user/1"
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "userById": {
                |       "name": "Frank"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "users",
                query = """
                | query (${'$'}v0: ID!) {
                |   userById(id: ${'$'}v0) {
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "ari:cloud:identity::user/2"
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "userById": {
                |       "name": "Tom"
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
     *     "issueByKey": {
     *       "key": "GQLGW-2",
     *       "related": [
     *         {
     *           "assignee": {
     *             "name": "Frank"
     *           }
     *         }
     *       ],
     *       "assignee": {
     *         "name": "Tom"
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
            |     "issueByKey": {
            |       "key": "GQLGW-2",
            |       "related": [
            |         {}
            |       ]
            |     }
            |   },
            |   "hasNext": true
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
                """
                | {
                |   "hasNext": false,
                |   "incremental": [
                |     {
                |       "path": [
                |         "issueByKey",
                |         "related",
                |         0
                |       ],
                |       "data": {
                |         "assignee": {
                |           "name": "Frank"
                |         }
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
                """
                | {
                |   "hasNext": true,
                |   "incremental": [
                |     {
                |       "path": [
                |         "issueByKey"
                |       ],
                |       "data": {
                |         "assignee": {
                |           "name": "Tom"
                |         }
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
            ),
        )
}
