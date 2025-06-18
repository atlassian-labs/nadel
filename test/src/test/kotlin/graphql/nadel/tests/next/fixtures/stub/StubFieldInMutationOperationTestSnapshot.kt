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
    graphql.nadel.tests.next.update<StubFieldInMutationOperationTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class StubFieldInMutationOperationTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "myService",
                query = """
                | mutation {
                |   createLlmBackedIssue(input: {prompt : "Need tests for stubbed fields in Mutation context"}) {
                |     issue {
                |       __typename__stubbed__key: __typename
                |       id
                |       title
                |     }
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
                |       "issue": {
                |         "id": "123",
                |         "__typename__stubbed__key": "Issue",
                |         "title": "Wow an issue"
                |       }
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
     *       "issue": {
     *         "id": "123",
     *         "title": "Wow an issue",
     *         "key": null
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
            |     "createLlmBackedIssue": {
            |       "success": true,
            |       "issue": {
            |         "id": "123",
            |         "title": "Wow an issue",
            |         "key": null
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
