// @formatter:off
package graphql.nadel.tests.next.fixtures.defer

import graphql.nadel.tests.next.ExpectedNadelResponse
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class UnderlyingServiceDeferTestData : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "echo",
                query = """
                | {
                |   echo
                | }
                """.trimMargin(),
                variables = "{}",
                response = """
                | {
                |   "echo": "Hello World"
                | }
                """.trimMargin(),
                delayedResponses = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "users",
                query = """
                | {
                |   users {
                |     name
                |     ... @defer {
                |       friends {
                |         name
                |         ... @defer {
                |           friends {
                |             name
                |           }
                |         }
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                response = """
                | {
                |   "users": [
                |     {
                |       "name": "Johnny"
                |     },
                |     {
                |       "name": "Bert"
                |     }
                |   ]
                | }
                """.trimMargin(),
                delayedResponses = listOfJsonStrings(
                    """
                    | {
                    |   "hasNext": false,
                    |   "incremental": [
                    |     {
                    |       "path": [
                    |         "users",
                    |         1,
                    |         "friends",
                    |         0
                    |       ],
                    |       "data": {
                    |         "friends": [
                    |           {
                    |             "name": "Bert"
                    |           }
                    |         ]
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
                    |         "users",
                    |         0
                    |       ],
                    |       "data": {
                    |         "friends": [
                    |           {
                    |             "name": "Bert"
                    |           }
                    |         ]
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
                    |         "users",
                    |         0,
                    |         "friends",
                    |         0
                    |       ],
                    |       "data": {
                    |         "friends": [
                    |           {
                    |             "name": "Johnny"
                    |           }
                    |         ]
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
                    |         "users",
                    |         1
                    |       ],
                    |       "data": {
                    |         "friends": [
                    |           {
                    |             "name": "Johnny"
                    |           }
                    |         ]
                    |       }
                    |     }
                    |   ]
                    | }
                    """.trimMargin(),
                ),
            ),
        )

    /**
     * ```json
     * {
     *   "data": {
     *     "echo": "Hello World",
     *     "users": [
     *       {
     *         "name": "Johnny",
     *         "friends": [
     *           {
     *             "name": "Bert",
     *             "friends": [
     *               {
     *                 "name": "Johnny"
     *               }
     *             ]
     *           }
     *         ]
     *       },
     *       {
     *         "name": "Bert",
     *         "friends": [
     *           {
     *             "name": "Johnny",
     *             "friends": [
     *               {
     *                 "name": "Bert"
     *               }
     *             ]
     *           }
     *         ]
     *       }
     *     ]
     *   }
     * }
     * ```
     */
    override val response: ExpectedNadelResponse = ExpectedNadelResponse(
            response = """
            | {
            |   "data": {
            |     "echo": "Hello World",
            |     "users": [
            |       {
            |         "name": "Johnny"
            |       },
            |       {
            |         "name": "Bert"
            |       }
            |     ]
            |   },
            |   "hasNext": true
            | }
            """.trimMargin(),
            delayedResponses = listOfJsonStrings(
                """
                | {
                |   "hasNext": false,
                |   "incremental": [
                |     {
                |       "path": [
                |         "users",
                |         1,
                |         "friends",
                |         0
                |       ],
                |       "data": {
                |         "friends": [
                |           {
                |             "name": "Bert"
                |           }
                |         ]
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
                |         "users",
                |         0
                |       ],
                |       "data": {
                |         "friends": [
                |           {
                |             "name": "Bert"
                |           }
                |         ]
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
                |         "users",
                |         0,
                |         "friends",
                |         0
                |       ],
                |       "data": {
                |         "friends": [
                |           {
                |             "name": "Johnny"
                |           }
                |         ]
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
                |         "users",
                |         1
                |       ],
                |       "data": {
                |         "friends": [
                |           {
                |             "name": "Johnny"
                |           }
                |         ]
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
            ),
        )
}
