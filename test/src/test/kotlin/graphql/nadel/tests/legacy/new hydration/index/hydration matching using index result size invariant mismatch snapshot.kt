// @formatter:off
package graphql.nadel.tests.legacy.`new hydration`.index

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings

private suspend fun main() {
    graphql.nadel.tests.next.update<`hydration matching using index result size invariant mismatch`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
class `hydration matching using index result size invariant mismatch snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> =
        listOf(
            ExpectedServiceCall(
                service = "Issues",
                query = """
                    query {
                      issues {
                        __typename__batch_hydration__authors: __typename
                        batch_hydration__authors__authorIds: authorIds
                        id
                      }
                    }
                """.trimIndent(),
                variables = "{}",
                result = """
                    {
                      "data": {
                        "issues": [
                          {
                            "__typename__batch_hydration__authors": "Issue",
                            "batch_hydration__authors__authorIds": [
                              "1"
                            ],
                            "id": "ISSUE-1"
                          },
                          {
                            "__typename__batch_hydration__authors": "Issue",
                            "batch_hydration__authors__authorIds": [
                              "1",
                              "2"
                            ],
                            "id": "ISSUE-2"
                          }
                        ]
                      }
                    }
                """.trimIndent(),
                delayedResults = listOfJsonStrings(),
            ),
            ExpectedServiceCall(
                service = "UserService",
                query = """
                    query {
                      usersByIds(ids: ["1", "2"]) {
                        name
                      }
                    }
                """.trimIndent(),
                variables = "{}",
                result = """
                    {
                      "data": {
                        "usersByIds": [
                          {
                            "name": "Name"
                          }
                        ]
                      }
                    }
                """.trimIndent(),
                delayedResults = listOfJsonStrings(),
            ),
        )

    /**
     * ```json
     * {}
     * ```
     */
    override val result: ExpectedNadelResult =
        ExpectedNadelResult(
            result = "{}",
            delayedResults = listOfJsonStrings(),
        )
}
