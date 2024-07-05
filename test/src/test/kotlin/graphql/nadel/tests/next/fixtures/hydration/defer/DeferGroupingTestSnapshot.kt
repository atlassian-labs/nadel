// @formatter:off
package graphql.nadel.tests.next.fixtures.hydration.defer

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<DeferGroupingTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class DeferGroupingTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "monolith",
                query = """
                | {
                |   issue(id: 1) {
                |     ... @defer {
                |       key
                |       id
                |     }
                |     ... @defer {
                |       key
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issue": {}
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
                    |         "issue"
                    |       ],
                    |       "data": {
                    |         "id": "1",
                    |         "key": "TEST-1"
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
                    |         "issue"
                    |       ],
                    |       "data": {
                    |         "key": "TEST-1"
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
     *     "issue": {
     *       "key": "TEST-1",
     *       "id": "1"
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "issue": {}
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
                |         "issue"
                |       ],
                |       "data": {
                |         "id": "1",
                |         "key": "TEST-1"
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
                |         "issue"
                |       ],
                |       "data": {
                |         "key": "TEST-1"
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
            ),
        )
}
