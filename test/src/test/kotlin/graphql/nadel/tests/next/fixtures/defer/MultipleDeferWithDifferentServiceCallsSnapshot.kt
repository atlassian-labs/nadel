// @formatter:off
package graphql.nadel.tests.next.fixtures.defer

import graphql.nadel.tests.next.ExpectedNadelResult
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
public class MultipleDeferWithDifferentServiceCallsSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "product",
                query = """
                | {
                |   product {
                |     productName
                |     ... @defer {
                |       productImage
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "errors": [
                |     {
                |       "message": "Validation error (UnknownDirective@[product]) : Unknown directive 'defer'",
                |       "locations": [
                |         {
                |           "line": 4,
                |           "column": 9
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
            ExpectedServiceCall(
                service = "users",
                query = """
                | {
                |   user {
                |     name
                |     ... @defer {
                |       profilePicture
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "errors": [
                |     {
                |       "message": "Validation error (UnknownDirective@[user]) : Unknown directive 'defer'",
                |       "locations": [
                |         {
                |           "line": 4,
                |           "column": 9
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
     *       "message": "Validation error (UnknownDirective@[user]) : Unknown directive 'defer'",
     *       "locations": [
     *         {
     *           "line": 4,
     *           "column": 9
     *         }
     *       ],
     *       "extensions": {
     *         "classification": "ValidationError"
     *       }
     *     },
     *     {
     *       "message": "Validation error (UnknownDirective@[product]) : Unknown directive 'defer'",
     *       "locations": [
     *         {
     *           "line": 4,
     *           "column": 9
     *         }
     *       ],
     *       "extensions": {
     *         "classification": "ValidationError"
     *       }
     *     }
     *   ],
     *   "data": {
     *     "user": null,
     *     "product": null
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "errors": [
            |     {
            |       "message": "Validation error (UnknownDirective@[user]) : Unknown directive 'defer'",
            |       "locations": [
            |         {
            |           "line": 4,
            |           "column": 9
            |         }
            |       ],
            |       "extensions": {
            |         "classification": "ValidationError"
            |       }
            |     },
            |     {
            |       "message": "Validation error (UnknownDirective@[product]) : Unknown directive 'defer'",
            |       "locations": [
            |         {
            |           "line": 4,
            |           "column": 9
            |         }
            |       ],
            |       "extensions": {
            |         "classification": "ValidationError"
            |       }
            |     }
            |   ],
            |   "data": {
            |     "user": null,
            |     "product": null
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
