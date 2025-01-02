// @formatter:off
package graphql.nadel.tests.legacy.renames

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`correct typename is returned`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `correct typename is returned snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "MyService",
                query = """
                | query {
                |   typenameTest {
                |     __typename
                |     object {
                |       __typename
                |     }
                |     objects {
                |       __typename
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "typenameTest": {
                |       "__typename": "TypenameTest",
                |       "object": {
                |         "__typename": "ObjectUnderlying"
                |       },
                |       "objects": [
                |         {
                |           "__typename": "ObjectUnderlying"
                |         },
                |         {
                |           "__typename": "ObjectUnderlying"
                |         }
                |       ]
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
        )

    /**
     * ```json
     * {
     *   "data": {
     *     "typenameTest": {
     *       "__typename": "TypenameTest",
     *       "object": {
     *         "__typename": "ObjectOverall"
     *       },
     *       "objects": [
     *         {
     *           "__typename": "ObjectOverall"
     *         },
     *         {
     *           "__typename": "ObjectOverall"
     *         }
     *       ]
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "typenameTest": {
            |       "__typename": "TypenameTest",
            |       "object": {
            |         "__typename": "ObjectOverall"
            |       },
            |       "objects": [
            |         {
            |           "__typename": "ObjectOverall"
            |         },
            |         {
            |           "__typename": "ObjectOverall"
            |         }
            |       ]
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
