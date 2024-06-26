// @formatter:off
package graphql.nadel.tests.next.fixtures.defer

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class MultipleDeferWithDifferentServiceCallsSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "product",
                query = """
                | {
                |   product {
                |     productName
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
                |       "productName": "Awesome Product"
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
     *       "profilePicture": "https://examplesite.com/user/profile_picture.jpg"
     *     },
     *     "product": {
     *       "productName": "Awesome Product",
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
            |       "productName": "Awesome Product"
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
