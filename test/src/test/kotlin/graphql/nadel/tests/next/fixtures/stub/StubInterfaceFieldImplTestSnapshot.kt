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
    graphql.nadel.tests.next.update<StubInterfaceFieldImplTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class StubInterfaceFieldImplTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "myService",
                query = """
                | {
                |   issue {
                |     ... on Issue {
                |       key
                |     }
                |     ... on Task {
                |       __typename__stubbed__key: __typename
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "issue": {
                |       "__typename__stubbed__key": "Task"
                |     }
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
                |   myWork {
                |     ... on Issue {
                |       key
                |     }
                |     ... on Task {
                |       __typename__stubbed__key: __typename
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "myWork": [
                |       {
                |         "__typename__stubbed__key": "Task"
                |       },
                |       {
                |         "key": "HELLO"
                |       },
                |       {
                |         "key": "BYE"
                |       },
                |       null,
                |       null,
                |       null
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
                |   task {
                |     ... on Issue {
                |       key
                |       whatsTheIssue
                |     }
                |     ... on Task {
                |       __typename__stubbed__key: __typename
                |       whatTodo
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "task": {
                |       "__typename__stubbed__key": "Task",
                |       "whatTodo": "Implement task key"
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
     *     "myWork": [
     *       {
     *         "key": null
     *       },
     *       {
     *         "key": "HELLO"
     *       },
     *       {
     *         "key": "BYE"
     *       },
     *       null,
     *       null,
     *       null
     *     ],
     *     "issue": {
     *       "key": null
     *     },
     *     "task": {
     *       "whatTodo": "Implement task key",
     *       "key": null
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "myWork": [
            |       {
            |         "key": null
            |       },
            |       {
            |         "key": "HELLO"
            |       },
            |       {
            |         "key": "BYE"
            |       },
            |       null,
            |       null,
            |       null
            |     ],
            |     "issue": {
            |       "key": null
            |     },
            |     "task": {
            |       "whatTodo": "Implement task key",
            |       "key": null
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
