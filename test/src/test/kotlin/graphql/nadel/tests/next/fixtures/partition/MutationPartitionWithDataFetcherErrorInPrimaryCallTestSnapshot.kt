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
    graphql.nadel.tests.next.update<MutationPartitionWithDataFetcherErrorInPrimaryCallTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class MutationPartitionWithDataFetcherErrorInPrimaryCallTestSnapshot : TestSnapshot() {
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
                |   "errors": [
                |     {
                |       "message": "Exception while fetching data (/thingsApi/linkThings) : Could not link things in partition-A",
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
                |         "success": true,
                |         "errors": [],
                |         "linkedThings": [
                |           {
                |             "id": "thing-3:partition-B",
                |             "name": "THING-3:PARTITION-B"
                |           },
                |           {
                |             "id": "thing-4:partition-B",
                |             "name": "THING-4:PARTITION-B"
                |           }
                |         ]
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
                |   "data": {
                |     "thingsApi": {
                |       "linkThings": {
                |         "success": true,
                |         "errors": [],
                |         "linkedThings": [
                |           {
                |             "id": "thing-5:partition-C",
                |             "name": "THING-5:PARTITION-C"
                |           },
                |           {
                |             "id": "thing-6:partition-C",
                |             "name": "THING-6:PARTITION-C"
                |           }
                |         ]
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
                |   "data": {
                |     "thingsApi": {
                |       "linkThings": {
                |         "success": true,
                |         "errors": [],
                |         "linkedThings": [
                |           {
                |             "id": "thing-7:partition-D",
                |             "name": "THING-7:PARTITION-D"
                |           },
                |           {
                |             "id": "thing-8:partition-D",
                |             "name": "THING-8:PARTITION-D"
                |           }
                |         ]
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
     *   "errors": [
     *     {
     *       "message": "Exception while fetching data (/thingsApi/linkThings) : Could not link
     * things in partition-A",
     *       "locations": [
     *         {
     *           "line": 3,
     *           "column": 5
     *         }
     *       ],
     *       "path": [
     *         "thingsApi",
     *         "linkThings"
     *       ],
     *       "extensions": {
     *         "classification": "DataFetchingException"
     *       }
     *     }
     *   ],
     *   "data": {
     *     "thingsApi": {
     *       "linkThings": {
     *         "success": false,
     *         "errors": [],
     *         "linkedThings": [
     *           {
     *             "id": "thing-3:partition-B",
     *             "name": "THING-3:PARTITION-B"
     *           },
     *           {
     *             "id": "thing-4:partition-B",
     *             "name": "THING-4:PARTITION-B"
     *           },
     *           {
     *             "id": "thing-5:partition-C",
     *             "name": "THING-5:PARTITION-C"
     *           },
     *           {
     *             "id": "thing-6:partition-C",
     *             "name": "THING-6:PARTITION-C"
     *           },
     *           {
     *             "id": "thing-7:partition-D",
     *             "name": "THING-7:PARTITION-D"
     *           },
     *           {
     *             "id": "thing-8:partition-D",
     *             "name": "THING-8:PARTITION-D"
     *           }
     *         ]
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
            |       "message": "Exception while fetching data (/thingsApi/linkThings) : Could not link things in partition-A",
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
            |       "linkThings": {
            |         "success": false,
            |         "errors": [],
            |         "linkedThings": [
            |           {
            |             "id": "thing-3:partition-B",
            |             "name": "THING-3:PARTITION-B"
            |           },
            |           {
            |             "id": "thing-4:partition-B",
            |             "name": "THING-4:PARTITION-B"
            |           },
            |           {
            |             "id": "thing-5:partition-C",
            |             "name": "THING-5:PARTITION-C"
            |           },
            |           {
            |             "id": "thing-6:partition-C",
            |             "name": "THING-6:PARTITION-C"
            |           },
            |           {
            |             "id": "thing-7:partition-D",
            |             "name": "THING-7:PARTITION-D"
            |           },
            |           {
            |             "id": "thing-8:partition-D",
            |             "name": "THING-8:PARTITION-D"
            |           }
            |         ]
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
