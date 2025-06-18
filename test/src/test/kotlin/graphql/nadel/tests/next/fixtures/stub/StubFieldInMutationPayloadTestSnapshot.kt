// @formatter:off
package graphql.nadel.tests.next.fixtures.stub

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<StubFieldInMutationPayloadTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class StubFieldInMutationPayloadTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "myService",
                query = """
                | mutation {
                |   createLlmBackedIssue(input: {prompt : "Need tests for stubbed fields in Mutation context"}) {
                |     __typename__stubbed__issue: __typename
                |     success
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "createLlmBackedIssue": {
                |       "success": true,
                |       "__typename__stubbed__issue": "CreateLlmBackedIssuePayload"
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
     *     "createLlmBackedIssue": {
     *       "success": true,
     *       "issue": null
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "createLlmBackedIssue": {
            |       "success": true,
            |       "issue": null
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
