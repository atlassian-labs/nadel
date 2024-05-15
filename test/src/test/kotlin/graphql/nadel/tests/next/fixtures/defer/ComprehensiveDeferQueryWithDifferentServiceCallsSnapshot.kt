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
public class ComprehensiveDeferQueryWithDifferentServiceCallsSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "product",
                query = """
                | {
                |   product {
                |     productName
                |     productDescription
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
                |           "line": 5,
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
                |     ... @defer(label: "team-details") {
                |       teamName
                |       teamMembers
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
                |     },
                |     {
                |       "message": "Validation error (UnknownDirective@[user]) : Unknown directive 'defer'",
                |       "locations": [
                |         {
                |           "line": 7,
                |           "column": 9
                |         }
                |       ],
                |       "extensions": {
                |         "classification": "ValidationError"
                |       }
                |     },
                |     {
                |       "message": "Validation error (UnknownArgument@[user]) : Unknown field argument 'label'",
                |       "locations": [
                |         {
                |           "line": 7,
                |           "column": 16
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
     *       "message": "Validation error (UnknownDirective@[user]) : Unknown directive 'defer'",
     *       "locations": [
     *         {
     *           "line": 7,
     *           "column": 9
     *         }
     *       ],
     *       "extensions": {
     *         "classification": "ValidationError"
     *       }
     *     },
     *     {
     *       "message": "Validation error (UnknownArgument@[user]) : Unknown field argument
     * 'label'",
     *       "locations": [
     *         {
     *           "line": 7,
     *           "column": 16
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
     *           "line": 5,
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
            |       "message": "Validation error (UnknownDirective@[user]) : Unknown directive 'defer'",
            |       "locations": [
            |         {
            |           "line": 7,
            |           "column": 9
            |         }
            |       ],
            |       "extensions": {
            |         "classification": "ValidationError"
            |       }
            |     },
            |     {
            |       "message": "Validation error (UnknownArgument@[user]) : Unknown field argument 'label'",
            |       "locations": [
            |         {
            |           "line": 7,
            |           "column": 16
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
            |           "line": 5,
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
