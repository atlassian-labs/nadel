// @formatter:off
package graphql.nadel.tests.next.fixtures.defer.transforms

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<RenamedTypeIsDeferredTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class RenamedTypeIsDeferredTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "defer",
                query = """
                | {
                |   zoo {
                |     cat {
                |       name
                |       ... @defer {
                |         __typename
                |       }
                |     }
                |     ... @defer {
                |       monkey {
                |         name
                |         __typename
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "zoo": {
                |       "cat": {
                |         "name": "Garfield"
                |       }
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
                    |         "zoo",
                    |         "cat"
                    |       ],
                    |       "data": {
                    |         "__typename": "Rat"
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
                    |         "zoo"
                    |       ],
                    |       "data": {
                    |         "monkey": {
                    |           "name": "Harambe",
                    |           "__typename": "Donkey"
                    |         }
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
     *     "zoo": {
     *       "cat": {
     *         "name": "Garfield",
     *         "__typename": "Cat"
     *       },
     *       "monkey": {
     *         "name": "Harambe",
     *         "__typename": "Monkey"
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
            |     "zoo": {
            |       "cat": {
            |         "name": "Garfield"
            |       }
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
                |         "zoo",
                |         "cat"
                |       ],
                |       "data": {
                |         "__typename": "Cat"
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
                |         "zoo"
                |       ],
                |       "data": {
                |         "monkey": {
                |           "name": "Harambe",
                |           "__typename": "Monkey"
                |         }
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
            ),
        )
}
