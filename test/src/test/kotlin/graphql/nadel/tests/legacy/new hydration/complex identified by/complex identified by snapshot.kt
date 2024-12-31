// @formatter:off
package graphql.nadel.tests.legacy.`new hydration`.`complex identified by`

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`complex identified by`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `complex identified by snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Issues",
                query = """
                | {
                |   issues {
                |     __typename__batch_hydration__author: __typename
                |     batch_hydration__author__authorId: authorId {
                |       userId
                |     }
                |     batch_hydration__author__authorId: authorId {
                |       site
                |     }
                |     id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issues": [
                |       {
                |         "id": "ISSUE-1",
                |         "batch_hydration__author__authorId": {
                |           "userId": "USER-1",
                |           "site": "hello"
                |         },
                |         "__typename__batch_hydration__author": "Issue"
                |       },
                |       {
                |         "id": "ISSUE-2",
                |         "batch_hydration__author__authorId": {
                |           "userId": "USER-3",
                |           "site": "hello"
                |         },
                |         "__typename__batch_hydration__author": "Issue"
                |       },
                |       {
                |         "id": "ISSUE-3",
                |         "batch_hydration__author__authorId": {
                |           "userId": "USER-2",
                |           "site": "jdog"
                |         },
                |         "__typename__batch_hydration__author": "Issue"
                |       },
                |       {
                |         "id": "ISSUE-4",
                |         "batch_hydration__author__authorId": {
                |           "userId": "USER-4",
                |           "site": "hello"
                |         },
                |         "__typename__batch_hydration__author": "Issue"
                |       },
                |       {
                |         "id": "ISSUE-5",
                |         "batch_hydration__author__authorId": {
                |           "userId": "USER-5",
                |           "site": "hello"
                |         },
                |         "__typename__batch_hydration__author": "Issue"
                |       },
                |       {
                |         "id": "ISSUE-6",
                |         "batch_hydration__author__authorId": {
                |           "userId": "USER-2",
                |           "site": "jdog"
                |         },
                |         "__typename__batch_hydration__author": "Issue"
                |       },
                |       {
                |         "id": "ISSUE-7",
                |         "batch_hydration__author__authorId": {
                |           "userId": "USER-2",
                |           "site": "hello"
                |         },
                |         "__typename__batch_hydration__author": "Issue"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "UserService",
                query = """
                | {
                |   users(id: [{site : "hello", userId : "USER-1"}, {site : "hello", userId : "USER-3"}, {site : "jdog", userId : "USER-2"}, {site : "hello", userId : "USER-4"}]) {
                |     id
                |     batch_hydration__author__id: id
                |     name
                |     batch_hydration__author__siteId: siteId
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "users": [
                |       {
                |         "id": "USER-1",
                |         "name": "H-One",
                |         "batch_hydration__author__id": "USER-1",
                |         "batch_hydration__author__siteId": "hello"
                |       },
                |       {
                |         "id": "USER-3",
                |         "name": "H-Three",
                |         "batch_hydration__author__id": "USER-3",
                |         "batch_hydration__author__siteId": "hello"
                |       },
                |       {
                |         "id": "USER-2",
                |         "name": "J-Two",
                |         "batch_hydration__author__id": "USER-2",
                |         "batch_hydration__author__siteId": "jdog"
                |       },
                |       {
                |         "id": "USER-4",
                |         "name": "H-Four",
                |         "batch_hydration__author__id": "USER-4",
                |         "batch_hydration__author__siteId": "hello"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "UserService",
                query = """
                | {
                |   users(id: [{site : "hello", userId : "USER-5"}, {site : "hello", userId : "USER-2"}]) {
                |     id
                |     batch_hydration__author__id: id
                |     name
                |     batch_hydration__author__siteId: siteId
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "users": [
                |       {
                |         "id": "USER-5",
                |         "name": "H-Five",
                |         "batch_hydration__author__id": "USER-5",
                |         "batch_hydration__author__siteId": "hello"
                |       },
                |       {
                |         "id": "USER-2",
                |         "name": "H-Two",
                |         "batch_hydration__author__id": "USER-2",
                |         "batch_hydration__author__siteId": "hello"
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
     *         "id": "ISSUE-1",
     *         "author": {
     *           "id": "USER-1",
     *           "name": "H-One"
     *         }
     *       },
     *       {
     *         "id": "ISSUE-2",
     *         "author": {
     *           "id": "USER-3",
     *           "name": "H-Three"
     *         }
     *       },
     *       {
     *         "id": "ISSUE-3",
     *         "author": {
     *           "id": "USER-2",
     *           "name": "J-Two"
     *         }
     *       },
     *       {
     *         "id": "ISSUE-4",
     *         "author": {
     *           "id": "USER-4",
     *           "name": "H-Four"
     *         }
     *       },
     *       {
     *         "id": "ISSUE-5",
     *         "author": {
     *           "id": "USER-5",
     *           "name": "H-Five"
     *         }
     *       },
     *       {
     *         "id": "ISSUE-6",
     *         "author": {
     *           "id": "USER-2",
     *           "name": "J-Two"
     *         }
     *       },
     *       {
     *         "id": "ISSUE-7",
     *         "author": {
     *           "id": "USER-2",
     *           "name": "H-Two"
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
            |         "id": "ISSUE-1",
            |         "author": {
            |           "id": "USER-1",
            |           "name": "H-One"
            |         }
            |       },
            |       {
            |         "id": "ISSUE-2",
            |         "author": {
            |           "id": "USER-3",
            |           "name": "H-Three"
            |         }
            |       },
            |       {
            |         "id": "ISSUE-3",
            |         "author": {
            |           "id": "USER-2",
            |           "name": "J-Two"
            |         }
            |       },
            |       {
            |         "id": "ISSUE-4",
            |         "author": {
            |           "id": "USER-4",
            |           "name": "H-Four"
            |         }
            |       },
            |       {
            |         "id": "ISSUE-5",
            |         "author": {
            |           "id": "USER-5",
            |           "name": "H-Five"
            |         }
            |       },
            |       {
            |         "id": "ISSUE-6",
            |         "author": {
            |           "id": "USER-2",
            |           "name": "J-Two"
            |         }
            |       },
            |       {
            |         "id": "ISSUE-7",
            |         "author": {
            |           "id": "USER-2",
            |           "name": "H-Two"
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
