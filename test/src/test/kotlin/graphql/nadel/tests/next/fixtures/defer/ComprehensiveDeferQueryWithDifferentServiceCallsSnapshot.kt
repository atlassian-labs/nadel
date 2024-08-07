// @formatter:off
package graphql.nadel.tests.next.fixtures.defer

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<ComprehensiveDeferQueryWithDifferentServiceCalls>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class ComprehensiveDeferQueryWithDifferentServiceCallsSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "product",
                query = """
                | {
                |   product {
                |     productName
                |     productDescription
                |     ... @defer {
                |       productImage
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "product": {
                |       "productName": "Awesome Product",
                |       "productDescription": null
                |     }
                |   },
                |   "hasNext": true
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                    """
                    | {
                    |   "hasNext": false,
                    |   "incremental": [
                    |     {
                    |       "path": [
                    |         "product"
                    |       ],
                    |       "data": {
                    |         "productImage": null
                    |       }
                    |     }
                    |   ]
                    | }
                    """.trimMargin(),
                ),
            ),
            ExpectedServiceCall(
                service = "users",
                query = """
                | {
                |   user {
                |     name
                |     ... @defer {
                |       profilePicture
                |     }
                |     ... @defer(label: "team-details") {
                |       teamName
                |       teamMembers
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "user": {
                |       "name": "Steven"
                |     }
                |   },
                |   "hasNext": true
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                    """
                    | {
                    |   "hasNext": false,
                    |   "incremental": [
                    |     {
                    |       "path": [
                    |         "user"
                    |       ],
                    |       "label": "team-details",
                    |       "data": {
                    |         "teamName": "The Unicorns",
                    |         "teamMembers": [
                    |           "Felipe",
                    |           "Franklin",
                    |           "Juliano"
                    |         ]
                    |       }
                    |     }
                    |   ]
                    | }
                    """.trimMargin(),
                    """
                    | {
                    |   "hasNext": true,
                    |   "incremental": [
                    |     {
                    |       "path": [
                    |         "user"
                    |       ],
                    |       "data": {
                    |         "profilePicture": "https://examplesite.com/user/profile_picture.jpg"
                    |       }
                    |     }
                    |   ]
                    | }
                    """.trimMargin(),
                ),
            ),
        )

    /**
     * ```json
     * {
     *   "data": {
     *     "user": {
     *       "name": "Steven",
     *       "profilePicture": "https://examplesite.com/user/profile_picture.jpg",
     *       "teamName": "The Unicorns",
     *       "teamMembers": [
     *         "Felipe",
     *         "Franklin",
     *         "Juliano"
     *       ]
     *     },
     *     "product": {
     *       "productName": "Awesome Product",
     *       "productDescription": null,
     *       "productImage": null
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "user": {
            |       "name": "Steven"
            |     },
            |     "product": {
            |       "productName": "Awesome Product",
            |       "productDescription": null
            |     }
            |   },
            |   "hasNext": true
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
                """
                | {
                |   "hasNext": false,
                |   "incremental": [
                |     {
                |       "path": [
                |         "user"
                |       ],
                |       "label": "team-details",
                |       "data": {
                |         "teamName": "The Unicorns",
                |         "teamMembers": [
                |           "Felipe",
                |           "Franklin",
                |           "Juliano"
                |         ]
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
                """
                | {
                |   "hasNext": true,
                |   "incremental": [
                |     {
                |       "path": [
                |         "product"
                |       ],
                |       "data": {
                |         "productImage": null
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
                """
                | {
                |   "hasNext": true,
                |   "incremental": [
                |     {
                |       "path": [
                |         "user"
                |       ],
                |       "data": {
                |         "profilePicture": "https://examplesite.com/user/profile_picture.jpg"
                |       }
                |     }
                |   ]
                | }
                """.trimMargin(),
            ),
        )
}
