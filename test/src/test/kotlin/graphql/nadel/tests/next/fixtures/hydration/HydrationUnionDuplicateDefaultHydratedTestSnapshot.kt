// @formatter:off
package graphql.nadel.tests.next.fixtures.hydration

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<HydrationUnionDuplicateDefaultHydratedTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class HydrationUnionDuplicateDefaultHydratedTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "myService",
                query = """
                | {
                |   issues {
                |     __typename__batch_hydration__user: __typename
                |     batch_hydration__user__userId: userId
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issues": [
                |       {
                |         "batch_hydration__user__userId": "aoeu",
                |         "__typename__batch_hydration__user": "Issue"
                |       },
                |       {
                |         "batch_hydration__user__userId": "asdf",
                |         "__typename__batch_hydration__user": "Issue"
                |       },
                |       {
                |         "batch_hydration__user__userId": "wow",
                |         "__typename__batch_hydration__user": "Issue"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "myService",
                query = """
                | {
                |   users(ids: ["aoeu", "asdf", "wow"]) {
                |     __typename
                |     batch_hydration__user__id: id
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "users": [
                |       {
                |         "__typename": "AtlassianAccountUser",
                |         "batch_hydration__user__id": "aoeu"
                |       },
                |       {
                |         "__typename": "CustomerUser",
                |         "batch_hydration__user__id": "asdf"
                |       },
                |       null
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
     *         "user": {
     *           "__typename": "AtlassianAccountUser"
     *         }
     *       },
     *       {
     *         "user": {
     *           "__typename": "CustomerUser"
     *         }
     *       },
     *       {
     *         "user": null
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
            |         "user": {
            |           "__typename": "AtlassianAccountUser"
            |         }
            |       },
            |       {
            |         "user": {
            |           "__typename": "CustomerUser"
            |         }
            |       },
            |       {
            |         "user": null
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
