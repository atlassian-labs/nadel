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
    graphql.nadel.tests.next.update<BatchHydrationConditionalEnumConditionTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class BatchHydrationConditionalEnumConditionTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "service2",
                query = """
                | query (${'$'}v0: [ID]) {
                |   storyBarsById(ids: ${'$'}v0) {
                |     batch_hydration__bars__id: id
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": [
                |     "bar-id-1",
                |     "bar-id-2"
                |   ]
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "storyBarsById": [
                |       {
                |         "batch_hydration__bars__id": "bar-id-1",
                |         "name": "Story Bar 1"
                |       },
                |       {
                |         "batch_hydration__bars__id": "bar-id-2",
                |         "name": "Story Bar 2"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "service1",
                query = """
                | {
                |   foo {
                |     __typename__batch_hydration__bars: __typename
                |     batch_hydration__bars__barIds: barIds
                |     batch_hydration__bars__barIds: barIds
                |     batch_hydration__bars__type: type
                |     batch_hydration__bars__type: type
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "foo": {
                |       "__typename__batch_hydration__bars": "Foo",
                |       "batch_hydration__bars__barIds": [
                |         "bar-id-1",
                |         "bar-id-2"
                |       ],
                |       "batch_hydration__bars__type": "STORY"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
        )

    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "foo": {
            |       "bars": [
            |         {
            |           "name": "Story Bar 1"
            |         },
            |         {
            |           "name": "Story Bar 2"
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
