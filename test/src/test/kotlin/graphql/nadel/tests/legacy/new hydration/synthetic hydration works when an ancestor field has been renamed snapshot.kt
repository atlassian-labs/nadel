// @formatter:off
package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`synthetic hydration works when an ancestor field has been renamed`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `synthetic hydration works when an ancestor field has been renamed snapshot` :
        TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "IssueService",
                query = """
                | query {
                |   rename__devOpsRelationships__relationships: relationships {
                |     __typename__rename__devOpsNodes: __typename
                |     rename__devOpsNodes__nodes: nodes {
                |       __typename__hydration__devOpsIssue: __typename
                |       hydration__devOpsIssue__issueId: issueId
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "rename__devOpsRelationships__relationships": {
                |       "__typename__rename__devOpsNodes": "RelationshipConnection",
                |       "rename__devOpsNodes__nodes": [
                |         {
                |           "__typename__hydration__devOpsIssue": "Relationship",
                |           "hydration__devOpsIssue__issueId": "1"
                |         }
                |       ]
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "IssueService",
                query = """
                | query {
                |   syntheticIssue {
                |     issue(id: "1") {
                |       id
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "syntheticIssue": {
                |       "issue": {
                |         "id": "1"
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
     *     "devOpsRelationships": {
     *       "devOpsNodes": [
     *         {
     *           "devOpsIssue": {
     *             "id": "1"
     *           }
     *         }
     *       ]
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "devOpsRelationships": {
            |       "devOpsNodes": [
            |         {
            |           "devOpsIssue": {
            |             "id": "1"
            |           }
            |         }
            |       ]
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
