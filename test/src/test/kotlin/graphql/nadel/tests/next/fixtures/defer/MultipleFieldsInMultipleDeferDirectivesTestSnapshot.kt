// @formatter:off
package graphql.nadel.tests.next.fixtures.defer

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<MultipleFieldsInMultipleDeferDirectivesTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class MultipleFieldsInMultipleDeferDirectivesTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "defer",
                query = """
                | {
                |   defer {
                |     fastField
                |     ... @defer {
                |       slowField
                |       slowField2
                |     }
                |     ... @defer {
                |       slowField3
                |       slowField4
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "defer": {
                |       "fastField": "123"
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
                    |         "defer"
                    |       ],
                    |       "data": {
                    |         "slowField": "slowString",
                    |         "slowField2": "slowString2"
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
                    |         "defer"
                    |       ],
                    |       "data": {
                    |         "slowField4": "slowString4",
                    |         "slowField3": "slowString3"
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
     *       "fastField": "123",
     *       "slowField4": "slowString4",
     *       "slowField3": "slowString3",
     *       "slowField": "slowString",
     *       "slowField2": "slowString2"
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "defer": {
            |       "fastField": "123"
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
                |         "defer"
                |       ],
                |       "data": {
                |         "slowField": "slowString",
                |         "slowField2": "slowString2"
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
                |         "defer"
                |       ],
                |       "data": {
                |         "slowField4": "slowString4",
                |         "slowField3": "slowString3"
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
            ),
        )
}
