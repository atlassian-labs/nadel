// @formatter:off
package graphql.nadel.tests.next.fixtures.rename

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<RenamedSharedInputTypeTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class RenamedSharedInputTypeTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "confluence_something",
                query = """
                | query (${'$'}v0: ConfluenceLegacyPathType!) {
                |   something {
                |     users {
                |       profilePicture {
                |         path(type: ${'$'}v0)
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "ABSOLUTE"
                | }
                """.trimMargin(),
                result = """
                | {
                |   "errors": [
                |     {
                |       "message": "Validation error (UnknownType) : Unknown type 'ConfluenceLegacyPathType'",
                |       "locations": [
                |         {
                |           "line": 1,
                |           "column": 13
                |         }
                |       ],
                |       "extensions": {
                |         "classification": "ValidationError"
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
        )

    /**
     * ```json
     * {
     *   "errors": [
     *     {
     *       "message": "Validation error (UnknownType) : Unknown type 'ConfluenceLegacyPathType'",
     *       "locations": [
     *         {
     *           "line": 1,
     *           "column": 13
     *         }
     *       ],
     *       "extensions": {
     *         "classification": "ValidationError"
     *       }
     *     }
     *   ],
     *   "data": {
     *     "something": null
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "errors": [
            |     {
            |       "message": "Validation error (UnknownType) : Unknown type 'ConfluenceLegacyPathType'",
            |       "locations": [
            |         {
            |           "line": 1,
            |           "column": 13
            |         }
            |       ],
            |       "extensions": {
            |         "classification": "ValidationError"
            |       }
            |     }
            |   ],
            |   "data": {
            |     "something": null
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
