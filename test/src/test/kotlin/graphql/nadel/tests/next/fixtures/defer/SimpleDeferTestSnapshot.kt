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
public class SimpleDeferTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "defer",
                query = """
                | {
                |   defer {
                |     hello
                |     ... @defer(label: "slow-defer") {
                |       slow
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                response = " {}",
                delayedResponses = listOfJsonStrings(
                ),
            ),
        )

    /**
     * ```json
     * {
     *   "errors": [
     *     {
     *       "message": "Validation error (UnknownDirective@[defer]) : Unknown directive 'defer'",
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
     *       "message": "Validation error (UnknownArgument@[defer]) : Unknown field argument
     * 'label'",
     *       "locations": [
     *         {
     *           "line": 4,
     *           "column": 16
     *         }
     *       ],
     *       "extensions": {
     *         "classification": "ValidationError"
     *       }
     *     }
     *   ],
     *   "data": {
     *     "defer": null
     *   }
     * }
     * ```
     */
    override val response: ExpectedNadelResponse = ExpectedNadelResponse(
            response = """
            | {
            |   "errors": [
            |     {
            |       "message": "Validation error (UnknownDirective@[defer]) : Unknown directive 'defer'",
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
            |       "message": "Validation error (UnknownArgument@[defer]) : Unknown field argument 'label'",
            |       "locations": [
            |         {
            |           "line": 4,
            |           "column": 16
            |         }
            |       ],
            |       "extensions": {
            |         "classification": "ValidationError"
            |       }
            |     }
            |   ],
            |   "data": {
            |     "defer": null
            |   }
            | }
            """.trimMargin(),
            delayedResponses = listOfJsonStrings(
            ),
        )
}
