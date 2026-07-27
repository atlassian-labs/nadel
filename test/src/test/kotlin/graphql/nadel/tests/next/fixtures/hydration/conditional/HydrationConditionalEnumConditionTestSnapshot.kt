// @formatter:off
package graphql.nadel.tests.next.fixtures.hydration.conditional

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<HydrationConditionalEnumConditionTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class HydrationConditionalEnumConditionTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "service1",
                query = """
                | {
                |   foo {
                |     __typename__hydration__matchingBar: __typename
                |     __typename__hydration__nonMatchingBar: __typename
                |     hydration__matchingBar__matchingBarId: matchingBarId
                |     hydration__nonMatchingBar__nonMatchingBarId: nonMatchingBarId
                |     hydration__matchingBar__type: type
                |     hydration__nonMatchingBar__type: type
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "foo": {
                |       "hydration__matchingBar__matchingBarId": "matching-bar-id",
                |       "hydration__matchingBar__type": "BUG",
                |       "__typename__hydration__matchingBar": "Foo",
                |       "hydration__nonMatchingBar__nonMatchingBarId": "non-matching-bar-id",
                |       "hydration__nonMatchingBar__type": "BUG",
                |       "__typename__hydration__nonMatchingBar": "Foo"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "service2",
                query = """
                | query (${'$'}v0: ID) {
                |   barById(id: ${'$'}v0) {
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "matching-bar-id"
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "barById": {
                |       "name": "Matching Bar"
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
     *     "foo": {
     *       "nonMatchingBar": null,
     *       "matchingBar": {
     *         "name": "Matching Bar"
     *       }
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "foo": {
            |       "nonMatchingBar": null,
            |       "matchingBar": {
            |         "name": "Matching Bar"
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
