// @formatter:off
package graphql.nadel.tests.next.fixtures.partition

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<MutationPartitionWithErrorsOnlyTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class MutationPartitionWithErrorsOnlyTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "things_service",
                query = """
                | mutation linkABunchOfThings {
                |   thingsApi {
                |     linkThings(linkThingsInput: {thinksLinked : [{from : "thing-1:partition-A", to : "thing-2:partition-A"}]}) {
                |       success
                |       errors {
                |         message
                |       }
                |       linkedThings {
                |         id
                |         name
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "thingsApi": {
                |       "linkThings": {
                |         "success": false,
                |         "errors": [
                |           {
                |             "message": "Could not link things: [{from=thing-1:partition-A, to=thing-2:partition-A}]"
                |           }
                |         ],
                |         "linkedThings": null
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "things_service",
                query = """
                | mutation linkABunchOfThings {
                |   thingsApi {
                |     linkThings(linkThingsInput: {thinksLinked : [{from : "thing-3:partition-B", to : "thing-4:partition-B"}]}) {
                |       success
                |       errors {
                |         message
                |       }
                |       linkedThings {
                |         id
                |         name
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "thingsApi": {
                |       "linkThings": {
                |         "success": false,
                |         "errors": [
                |           {
                |             "message": "Could not link things: [{from=thing-3:partition-B, to=thing-4:partition-B}]"
                |           }
                |         ],
                |         "linkedThings": null
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "things_service",
                query = """
                | mutation linkABunchOfThings {
                |   thingsApi {
                |     linkThings(linkThingsInput: {thinksLinked : [{from : "thing-5:partition-C", to : "thing-6:partition-C"}]}) {
                |       success
                |       errors {
                |         message
                |       }
                |       linkedThings {
                |         id
                |         name
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "errors": [
                |     {
                |       "message": "Exception while fetching data (/thingsApi/linkThings) : Exception: Could not link things [{from=thing-5:partition-C, to=thing-6:partition-C}]",
                |       "locations": [
                |         {
                |           "line": 3,
                |           "column": 5
                |         }
                |       ],
                |       "path": [
                |         "thingsApi",
                |         "linkThings"
                |       ],
                |       "extensions": {
                |         "classification": "DataFetchingException"
                |       }
                |     }
                |   ],
                |   "data": {
                |     "thingsApi": {
                |       "linkThings": null
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "things_service",
                query = """
                | mutation linkABunchOfThings {
                |   thingsApi {
                |     linkThings(linkThingsInput: {thinksLinked : [{from : "thing-7:partition-D", to : "thing-8:partition-D"}]}) {
                |       success
                |       errors {
                |         message
                |       }
                |       linkedThings {
                |         id
                |         name
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "errors": [
                |     {
                |       "message": "Exception while fetching data (/thingsApi/linkThings) : Exception: Could not link things [{from=thing-7:partition-D, to=thing-8:partition-D}]",
                |       "locations": [
                |         {
                |           "line": 3,
                |           "column": 5
                |         }
                |       ],
                |       "path": [
                |         "thingsApi",
                |         "linkThings"
                |       ],
                |       "extensions": {
                |         "classification": "DataFetchingException"
                |       }
                |     }
                |   ],
                |   "data": {
                |     "thingsApi": {
                |       "linkThings": null
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
     *   "errors": [
     *     {
     *       "message": "Exception while fetching data (/thingsApi/linkThings) : Exception: Could
     * not link things [{from=thing-5:partition-C, to=thing-6:partition-C}]",
     *       "locations": [],
     *       "path": [
     *         "thingsApi",
     *         "linkThings"
     *       ],
     *       "extensions": {
     *         "classification": "DataFetchingException",
     *         "errorHappenedOnPartitionedCall": true
     *       }
     *     },
     *     {
     *       "message": "Exception while fetching data (/thingsApi/linkThings) : Exception: Could
     * not link things [{from=thing-7:partition-D, to=thing-8:partition-D}]",
     *       "locations": [],
     *       "path": [
     *         "thingsApi",
     *         "linkThings"
     *       ],
     *       "extensions": {
     *         "classification": "DataFetchingException",
     *         "errorHappenedOnPartitionedCall": true
     *       }
     *     }
     *   ],
     *   "data": {
     *     "thingsApi": {
     *       "linkThings": {
     *         "success": false,
     *         "errors": [
     *           {
     *             "message": "Could not link things: [{from=thing-1:partition-A,
     * to=thing-2:partition-A}]"
     *           },
     *           {
     *             "message": "Could not link things: [{from=thing-3:partition-B,
     * to=thing-4:partition-B}]"
     *           }
     *         ],
     *         "linkedThings": null
     *       }
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "errors": [
            |     {
            |       "message": "Exception while fetching data (/thingsApi/linkThings) : Exception: Could not link things [{from=thing-5:partition-C, to=thing-6:partition-C}]",
            |       "locations": [],
            |       "path": [
            |         "thingsApi",
            |         "linkThings"
            |       ],
            |       "extensions": {
            |         "classification": "DataFetchingException",
            |         "errorHappenedOnPartitionedCall": true
            |       }
            |     },
            |     {
            |       "message": "Exception while fetching data (/thingsApi/linkThings) : Exception: Could not link things [{from=thing-7:partition-D, to=thing-8:partition-D}]",
            |       "locations": [],
            |       "path": [
            |         "thingsApi",
            |         "linkThings"
            |       ],
            |       "extensions": {
            |         "classification": "DataFetchingException",
            |         "errorHappenedOnPartitionedCall": true
            |       }
            |     }
            |   ],
            |   "data": {
            |     "thingsApi": {
            |       "linkThings": {
            |         "success": false,
            |         "errors": [
            |           {
            |             "message": "Could not link things: [{from=thing-1:partition-A, to=thing-2:partition-A}]"
            |           },
            |           {
            |             "message": "Could not link things: [{from=thing-3:partition-B, to=thing-4:partition-B}]"
            |           }
            |         ],
            |         "linkedThings": null
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
