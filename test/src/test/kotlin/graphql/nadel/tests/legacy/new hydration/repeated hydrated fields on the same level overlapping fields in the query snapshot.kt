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
    graphql.nadel.tests.next.update<`repeated hydrated fields on the same level overlapping fields in the query`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `repeated hydrated fields on the same level overlapping fields in the query snapshot` :
        TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Foo",
                query = """
                | {
                |   foo {
                |     __typename__hydration__issue: __typename
                |     hydration__issue__issueId: issueId
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "foo": {
                |       "hydration__issue__issueId": "ISSUE-1",
                |       "__typename__hydration__issue": "Foo"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Foo",
                query = """
                | query (${'$'}v0: ID) {
                |   issue(issueId: ${'$'}v0) {
                |     desc
                |     name
                |     summary
                |   }
                | }
                """.trimMargin(),
                variables = """
                | {
                |   "v0": "ISSUE-1"
                | }
                """.trimMargin(),
                result = """
                | {
                |   "data": {
                |     "issue": {
                |       "name": "I AM A NAME",
                |       "summary": "I AM A SUMMARY",
                |       "desc": "I AM A DESC"
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
     *     "foo": {
     *       "issue": {
     *         "name": "I AM A NAME",
     *         "summary": "I AM A SUMMARY",
     *         "desc": "I AM A DESC"
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
            |     "foo": {
            |       "issue": {
            |         "name": "I AM A NAME",
            |         "summary": "I AM A SUMMARY",
            |         "desc": "I AM A DESC"
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
