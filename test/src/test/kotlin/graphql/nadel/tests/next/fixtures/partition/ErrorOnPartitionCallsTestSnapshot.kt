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
    graphql.nadel.tests.next.update<ErrorOnPartitionCallsTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class ErrorOnPartitionCallsTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "things_service",
                query = """
                | query getPartitionedThings {
                |   things(ids: ["thing-1:partition-A", "thing-3:partition-A"]) {
                |     id
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "things": [
                |       {
                |         "id": "thing-1",
                |         "name": "THING-1"
                |       },
                |       {
                |         "id": "thing-3",
                |         "name": "THING-3"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "things_service",
                query = """
                | query getPartitionedThings {
                |   things(ids: ["thing-2:partition-B", "thing-4:partition-B"]) {
                |     id
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "things": [
                |       {
                |         "id": "thing-2",
                |         "name": "THING-2"
                |       },
                |       {
                |         "id": "thing-4",
                |         "name": "THING-4"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "things_service",
                query = """
                | query getPartitionedThings {
                |   things(ids: ["thing-5:partition-C", "thing-7:partition-C"]) {
                |     id
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "errors": [
                |     {
                |       "message": "Exception while fetching data (/things) : Error fetching things: [thing-5:partition-C, thing-7:partition-C]",
                |       "locations": [
                |         {
                |           "line": 2,
                |           "column": 3
                |         }
                |       ],
                |       "path": [
                |         "things"
                |       ],
                |       "extensions": {
                |         "classification": "DataFetchingException"
                |       }
                |     }
                |   ],
                |   "data": {
                |     "things": null
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "things_service",
                query = """
                | query getPartitionedThings {
                |   things(ids: ["thing-6:partition-D", "thing-8:partition-D"]) {
                |     id
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "errors": [
                |     {
                |       "message": "Exception while fetching data (/things) : Error fetching things: [thing-6:partition-D, thing-8:partition-D]",
                |       "locations": [
                |         {
                |           "line": 2,
                |           "column": 3
                |         }
                |       ],
                |       "path": [
                |         "things"
                |       ],
                |       "extensions": {
                |         "classification": "DataFetchingException"
                |       }
                |     }
                |   ],
                |   "data": {
                |     "things": null
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
     *       "message": "Exception while fetching data (/things) : Error fetching things:
     * [thing-5:partition-C, thing-7:partition-C]",
     *       "locations": [],
     *       "path": [
     *         "things"
     *       ],
     *       "extensions": {
     *         "classification": "DataFetchingException",
     *         "errorHappenedOnPartitionedCall": true
     *       }
     *     },
     *     {
     *       "message": "Exception while fetching data (/things) : Error fetching things:
     * [thing-6:partition-D, thing-8:partition-D]",
     *       "locations": [],
     *       "path": [
     *         "things"
     *       ],
     *       "extensions": {
     *         "classification": "DataFetchingException",
     *         "errorHappenedOnPartitionedCall": true
     *       }
     *     }
     *   ],
     *   "data": {
     *     "things": [
     *       {
     *         "id": "thing-1",
     *         "name": "THING-1"
     *       },
     *       {
     *         "id": "thing-3",
     *         "name": "THING-3"
     *       },
     *       {
     *         "id": "thing-2",
     *         "name": "THING-2"
     *       },
     *       {
     *         "id": "thing-4",
     *         "name": "THING-4"
     *       }
     *     ]
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "errors": [
            |     {
            |       "message": "Exception while fetching data (/things) : Error fetching things: [thing-5:partition-C, thing-7:partition-C]",
            |       "locations": [],
            |       "path": [
            |         "things"
            |       ],
            |       "extensions": {
            |         "classification": "DataFetchingException",
            |         "errorHappenedOnPartitionedCall": true
            |       }
            |     },
            |     {
            |       "message": "Exception while fetching data (/things) : Error fetching things: [thing-6:partition-D, thing-8:partition-D]",
            |       "locations": [],
            |       "path": [
            |         "things"
            |       ],
            |       "extensions": {
            |         "classification": "DataFetchingException",
            |         "errorHappenedOnPartitionedCall": true
            |       }
            |     }
            |   ],
            |   "data": {
            |     "things": [
            |       {
            |         "id": "thing-1",
            |         "name": "THING-1"
            |       },
            |       {
            |         "id": "thing-3",
            |         "name": "THING-3"
            |       },
            |       {
            |         "id": "thing-2",
            |         "name": "THING-2"
            |       },
            |       {
            |         "id": "thing-4",
            |         "name": "THING-4"
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
