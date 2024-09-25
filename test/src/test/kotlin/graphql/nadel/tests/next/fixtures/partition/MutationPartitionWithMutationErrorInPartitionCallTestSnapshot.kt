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
    graphql.nadel.tests.next.update<MutationPartitionWithMutationErrorInPartitionCallTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class MutationPartitionWithMutationErrorInPartitionCallTestSnapshot : TestSnapshot() {
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
                |         "success": true,
                |         "errors": [],
                |         "linkedThings": [
                |           {
                |             "id": "thing-1:partition-A",
                |             "name": "THING-1:PARTITION-A"
                |           },
                |           {
                |             "id": "thing-2:partition-A",
                |             "name": "THING-2:PARTITION-A"
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
                |             "message": "Could not link things [{from=thing-3:partition-B, to=thing-4:partition-B}]"
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
                |   "data": {
                |     "thingsApi": {
                |       "linkThings": {
                |         "success": false,
                |         "errors": [
                |           {
                |             "message": "Could not link things [{from=thing-5:partition-C, to=thing-6:partition-C}]"
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
     *   "data": {
     *     "thingsApi": {
     *       "linkThings": {
     *         "success": false,
     *         "errors": [
     *           {
     *             "message": "Could not link things [{from=thing-3:partition-B,
     * to=thing-4:partition-B}]"
     *           },
     *           {
     *             "message": "Could not link things [{from=thing-5:partition-C,
     * to=thing-6:partition-C}]"
     *           }
     *         ],
     *         "linkedThings": [
     *           {
     *             "id": "thing-1:partition-A",
     *             "name": "THING-1:PARTITION-A"
     *           },
     *           {
     *             "id": "thing-2:partition-A",
     *             "name": "THING-2:PARTITION-A"
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
            |   "data": {
            |     "thingsApi": {
            |       "linkThings": {
            |         "success": false,
            |         "errors": [
            |           {
            |             "message": "Could not link things [{from=thing-3:partition-B, to=thing-4:partition-B}]"
            |           },
            |           {
            |             "message": "Could not link things [{from=thing-5:partition-C, to=thing-6:partition-C}]"
            |           }
            |         ],
            |         "linkedThings": [
            |           {
            |             "id": "thing-1:partition-A",
            |             "name": "THING-1:PARTITION-A"
            |           },
            |           {
            |             "id": "thing-2:partition-A",
            |             "name": "THING-2:PARTITION-A"
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
