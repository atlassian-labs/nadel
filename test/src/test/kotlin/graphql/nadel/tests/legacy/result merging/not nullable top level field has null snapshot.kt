// @formatter:off
package graphql.nadel.tests.legacy.`result merging`

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`not nullable top level field has null`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `not nullable top level field has null snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "service",
                query = """
                | {
                |   foo
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "errors": [
                |     {
                |       "message": "The field at path '/foo' was declared as a non null type, but the code involved in retrieving data has wrongly returned a null value.  The graphql specification requires that the parent field be set to null, or if that is non nullable that it bubble up null to its parent and so on. The non-nullable type is 'String' within parent type 'Query'",
                |       "path": [
                |         "foo"
                |       ],
                |       "extensions": {
                |         "classification": "NullValueInNonNullableField"
                |       }
                |     }
                |   ],
                |   "data": null
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
     *       "message": "The field at path '/foo' was declared as a non null type, but the code
     * involved in retrieving data has wrongly returned a null value.  The graphql specification
     * requires that the parent field be set to null, or if that is non nullable that it bubble up null
     * to its parent and so on. The non-nullable type is 'String' within parent type 'Query'",
     *       "locations": [],
     *       "path": [
     *         "foo"
     *       ],
     *       "extensions": {
     *         "classification": "NullValueInNonNullableField"
     *       }
     *     }
     *   ],
     *   "data": null
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "errors": [
            |     {
            |       "message": "The field at path '/foo' was declared as a non null type, but the code involved in retrieving data has wrongly returned a null value.  The graphql specification requires that the parent field be set to null, or if that is non nullable that it bubble up null to its parent and so on. The non-nullable type is 'String' within parent type 'Query'",
            |       "locations": [],
            |       "path": [
            |         "foo"
            |       ],
            |       "extensions": {
            |         "classification": "NullValueInNonNullableField"
            |       }
            |     }
            |   ],
            |   "data": null
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
