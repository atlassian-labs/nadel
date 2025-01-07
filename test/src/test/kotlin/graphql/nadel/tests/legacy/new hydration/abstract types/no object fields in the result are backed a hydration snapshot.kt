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
    graphql.nadel.tests.next.update<`no object fields in the result are backed a hydration`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `no object fields in the result are backed a hydration snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "activity",
                query = """
                | {
                |   activity {
                |     ... on Activity {
                |       __typename__hydration__user: __typename
                |       hydration__user__userId: userId
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
