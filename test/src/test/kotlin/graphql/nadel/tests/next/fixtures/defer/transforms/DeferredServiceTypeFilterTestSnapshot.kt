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
    graphql.nadel.tests.next.update<DeferredServiceTypeFilterTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class DeferredServiceTypeFilterTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            )

    /**
     * ```json
     * {
     *   "errors": [
     *     {
     *       "message": "Validation error (FieldUndefined@[aErrors]) : Field 'aErrors' in type
     * 'Query' is undefined",
     *       "locations": [
     *         {
     *           "line": 2,
     *           "column": 3
     *         }
     *       ],
     *       "extensions": {
     *         "classification": "ValidationError"
     *       }
     *     }
     *   ]
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "errors": [
            |     {
            |       "message": "Validation error (FieldUndefined@[aErrors]) : Field 'aErrors' in type 'Query' is undefined",
            |       "locations": [
            |         {
            |           "line": 2,
            |           "column": 3
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
        )
}
