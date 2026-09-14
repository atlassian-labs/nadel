// @formatter:off
package graphql.nadel.tests.next.fixtures.validation

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<QueryFieldCountLimitExceededTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class QueryFieldCountLimitExceededTestSnapshot : TestSnapshot() {
    /**
     * Query
     *
     * ```graphql
     * { echo { value } }
     * ```
     *
     * Variables
     *
     * ```json
     * {}
     * ```
     */
    override val calls: List<ExpectedServiceCall> = listOf(
            )

    /**
     * ```json
     * {
     *   "errors": [
     *     {
     *       "message": "Query has 2 fields which exceeds maximum allowed 1",
     *       "locations": [],
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
            |       "message": "Query has 2 fields which exceeds maximum allowed 1",
            |       "locations": [],
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
