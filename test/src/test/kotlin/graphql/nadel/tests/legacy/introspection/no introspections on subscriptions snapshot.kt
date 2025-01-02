// @formatter:off
package graphql.nadel.tests.legacy.introspection

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`no introspections on subscriptions`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `no introspections on subscriptions snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            )

    /**
     * ```json
     * {
     *   "data": null,
     *   "errors": [
     *     {
     *       "message": "Validation error (SubscriptionIntrospectionRootField) : Subscription
     * operation 'null' root field '__typename' cannot be an introspection field",
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
            |   "data": null,
            |   "errors": [
            |     {
            |       "message": "Validation error (SubscriptionIntrospectionRootField) : Subscription operation 'null' root field '__typename' cannot be an introspection field",
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
