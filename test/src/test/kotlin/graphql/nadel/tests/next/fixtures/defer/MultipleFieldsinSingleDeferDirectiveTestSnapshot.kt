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
public class MultipleFieldsinSingleDeferDirectiveTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "defer",
                query = """
                | {
                |   defer {
                |     fastField
                |     ... @defer {
                |       slowField
                |       anotherSlowField
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                response = """
                | {
                |   "defer": {
                |     "fastField": "123"
                |   }
                | }
                """.trimMargin(),
                delayedResponses = listOfJsonStrings(
                    """
                    | {
                    |   "hasNext": false,
                    |   "incremental": [
                    |     {
                    |       "path": [
                    |         "defer"
                    |       ],
                    |       "data": {
                    |         "slowField": "slowString",
                    |         "anotherSlowField": 123456789
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
     *     "defer": {
     *       "slowField": "slowString",
     *       "anotherSlowField": 123456789
     *     }
     *   }
     * }
     * ```
     */
    override val response: ExpectedNadelResponse = ExpectedNadelResponse(
            response = """
            | {
            |   "data": {
            |     "defer": {
            |       "fastField": "123"
            |     }
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
                |         "defer"
                |       ],
                |       "data": {
                |         "slowField": "slowString",
                |         "anotherSlowField": 123456789
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
            ),
        )
}
