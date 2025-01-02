// @formatter:off
package graphql.nadel.tests.legacy.scalars

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`hydrating using url as arg`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `hydrating using url as arg snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "service",
                query = """
                | {
                |   foo {
                |     __typename__hydration__details: __typename
                |     url
                |     hydration__details__url: url
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "foo": {
                |       "url": "https://github.com/atlassian-labs/nadel",
                |       "hydration__details__url": "https://github.com/atlassian-labs/nadel",
                |       "__typename__hydration__details": "Foo"
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "service",
                query = """
                | {
                |   lookup(url: "https://github.com/atlassian-labs/nadel") {
                |     baseUrl
                |     createdAt
                |     owner
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "lookup": {
                |       "baseUrl": "https://github.com/",
                |       "owner": "amarek",
                |       "createdAt": "2018-02-13T06:23:41Z"
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
     *       "url": "https://github.com/atlassian-labs/nadel",
     *       "details": {
     *         "baseUrl": "https://github.com/",
     *         "owner": "amarek",
     *         "createdAt": "2018-02-13T06:23:41Z"
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
            |       "url": "https://github.com/atlassian-labs/nadel",
            |       "details": {
            |         "baseUrl": "https://github.com/",
            |         "owner": "amarek",
            |         "createdAt": "2018-02-13T06:23:41Z"
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
