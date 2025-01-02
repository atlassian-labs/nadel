// @formatter:off
package graphql.nadel.tests.legacy.`new hydration`.`abstract types`

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`some renamed object types have fields in the result are backed a batch hydration`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class
        `some renamed object types have fields in the result are backed a batch hydration snapshot`
        : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "activity",
                query = """
                | {
                |   activity {
                |     ... on Activity {
                |       __typename__batch_hydration__user: __typename
                |       batch_hydration__user__userId: userId
                |     }
                |     ... on SingleActivity {
                |       user {
                |         name
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "activity": [
                |       {
                |         "user": {
                |           "name": "John"
                |         }
                |       },
                |       {
                |         "__typename__batch_hydration__user": "Activity",
                |         "batch_hydration__user__userId": "user-100"
                |       },
                |       {
                |         "__typename__batch_hydration__user": "Activity",
                |         "batch_hydration__user__userId": "user-20"
                |       },
                |       {
                |         "user": {
                |           "name": "Mayor"
                |         }
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "users",
                query = """
                | {
                |   usersByIds(ids: ["user-100", "user-20"]) {
                |     batch_hydration__user__id: id
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "usersByIds": [
                |       {
                |         "batch_hydration__user__id": "user-100",
                |         "name": "Spaces"
                |       },
                |       {
                |         "batch_hydration__user__id": "user-20",
                |         "name": "Newmarket"
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
     *     "activity": [
     *       {
     *         "user": {
     *           "name": "John"
     *         }
     *       },
     *       {
     *         "user": {
     *           "name": "Spaces"
     *         }
     *       },
     *       {
     *         "user": {
     *           "name": "Newmarket"
     *         }
     *       },
     *       {
     *         "user": {
     *           "name": "Mayor"
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
            |     "activity": [
            |       {
            |         "user": {
            |           "name": "John"
            |         }
            |       },
            |       {
            |         "user": {
            |           "name": "Spaces"
            |         }
            |       },
            |       {
            |         "user": {
            |           "name": "Newmarket"
            |         }
            |       },
            |       {
            |         "user": {
            |           "name": "Mayor"
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
