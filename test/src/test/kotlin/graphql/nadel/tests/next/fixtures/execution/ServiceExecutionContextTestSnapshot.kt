// @formatter:off
package graphql.nadel.tests.next.fixtures.execution

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings

private suspend fun main() {
    graphql.nadel.tests.next.update<ServiceExecutionContextTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused") class ServiceExecutionContextTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "monolith",
                query = """
                | {
                |   bug: issue(id: 6) {
                |     title
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "bug": {
                |       "title": "Fix cloud ID header not set"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "monolith",
                query = """
                | {
                |   issue(id: "9") {
                |     title
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issue": {
                |       "title": "Improve cloud ID extraction"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "monolith",
                query = """
                | {
                |   me {
                |     __typename__hydration__lastWorkedOn: __typename
                |     hydration__lastWorkedOn__lastWorkedOnId: lastWorkedOnId
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "me": {
                |       "hydration__lastWorkedOn__lastWorkedOnId": "9",
                |       "__typename__hydration__lastWorkedOn": "User"
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
     *     "bug": {
     *       "title": "Fix cloud ID header not set"
     *     },
     *     "me": {
     *       "lastWorkedOn": {
     *         "title": "Improve cloud ID extraction"
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
            |     "bug": {
            |       "title": "Fix cloud ID header not set"
            |     },
            |     "me": {
            |       "lastWorkedOn": {
            |         "title": "Improve cloud ID extraction"
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
