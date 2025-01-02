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
    graphql.nadel.tests.next.update<`some renamed object types have fields in the result are backed a hydration`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `some renamed object types have fields in the result are backed a hydration snapshot` :
        TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "activity",
                query = """
                | {
                |   activity {
                |     ... on SomethingLame {
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
                |         "__typename__hydration__user": "SomethingLame",
                |         "hydration__user__userId": "user-100"
                |       },
                |       {
                |         "__typename__hydration__user": "SomethingLame",
                |         "hydration__user__userId": "user-20"
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
                |   userById(id: "user-100") {
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "userById": {
                |       "name": "Hello"
                |     }
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
                |   userById(id: "user-20") {
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "userById": {
                |       "name": "World"
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
     *     "activity": [
     *       {
     *         "user": {
     *           "name": "John"
     *         }
     *       },
     *       {
     *         "user": {
     *           "name": "Hello"
     *         }
     *       },
     *       {
     *         "user": {
     *           "name": "World"
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
            |           "name": "Hello"
            |         }
            |       },
            |       {
            |         "user": {
            |           "name": "World"
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
