// @formatter:off
package graphql.nadel.tests.legacy.`chained transforms`.`ari use case`

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`ari argument in renamed input`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `ari argument in renamed input snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "MyService",
                query = """
                | mutation mainJiraSoftwareStartSprintModalSubmitMutation(${'$'}v0: SprintInput) {
                |   startSprint(input: ${'$'}v0) {
                |     __typename
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": {
                |     "boardId": "123",
                |     "sprintId": "456",
                |     "name": "Test Input",
                |     "goal": null,
                |     "startDate": "2022-03-22",
                |     "endDate": "2022-04-02"
                |   }
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "startSprint": {
                |       "__typename": "Sprint"
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
     *     "startSprint": {
     *       "__typename": "Sprint"
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "startSprint": {
            |       "__typename": "Sprint"
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
