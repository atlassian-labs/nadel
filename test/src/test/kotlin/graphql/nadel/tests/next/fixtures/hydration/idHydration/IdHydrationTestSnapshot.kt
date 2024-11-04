package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings

private suspend fun main() {
    graphql.nadel.tests.next.update<IdHydrationTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class IdHydrationTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
        ExpectedServiceCall(
            service = "Identity",
            query = """
                | {
                |   usersByIds(ids: ["ari:cloud:identity::user/1"]) {
                |     name
                |     batch_hydration__assignee__id: id
                |   }
                | }
                """.trimMargin(),
            variables = " {}",
            result = """
                | {
                |   "data": {
                |     "usersByIds": [
                |       {
                |         "name": "First",
                |         "batch_hydration__assignee__id": "ari:cloud:identity::user/1"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        ),
        ExpectedServiceCall(
            service = "Identity",
            query = """
                | {
                |   usersByIds(ids: ["ari:cloud:identity::user/128"]) {
                |     name
                |     batch_hydration__assignee__id: id
                |   }
                | }
                """.trimMargin(),
            variables = " {}",
            result = """
                | {
                |   "data": {
                |     "usersByIds": [
                |       {
                |         "name": "2^7",
                |         "batch_hydration__assignee__id": "ari:cloud:identity::user/128"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        ),
        ExpectedServiceCall(
            service = "Jira",
            query = """
                | {
                |   issues {
                |     batch_hydration__assignee__assigneeId: assigneeId
                |     __typename__batch_hydration__assignee: __typename
                |   }
                | }
                """.trimMargin(),
            variables = " {}",
            result = """
                | {
                |   "data": {
                |     "issues": [
                |       {
                |         "batch_hydration__assignee__assigneeId": "ari:cloud:identity::user/1",
                |         "__typename__batch_hydration__assignee": "Issue"
                |       },
                |       {
                |         "batch_hydration__assignee__assigneeId": "ari:cloud:identity::user/128",
                |         "__typename__batch_hydration__assignee": "Issue"
                |       }
                |     ]
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
     *     "issues": [
     *       {
     *         "assignee": {
     *           "name": "First"
     *         }
     *       },
     *       {
     *         "assignee": {
     *           "name": "2^7"
     *         }
     *       }
     *     ]
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
        result = """
            | {
            |   "data": {
            |     "issues": [
            |       {
            |         "assignee": {
            |           "name": "First"
            |         }
            |       },
            |       {
            |         "assignee": {
            |           "name": "2^7"
            |         }
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
        delayedResults = listOfJsonStrings(
        ),
    )
}
