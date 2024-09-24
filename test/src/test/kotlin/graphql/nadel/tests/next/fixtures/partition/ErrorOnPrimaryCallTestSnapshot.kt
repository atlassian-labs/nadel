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
    graphql.nadel.tests.next.update<ErrorOnPrimaryCallTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class ErrorOnPrimaryCallTestSnapshot : TestSnapshot() {
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
                |   "errors": [
                |     {
                |       "message": "Exception while fetching data (/things) : Error fetching things: [thing-1:partition-A, thing-3:partition-A]",
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
                |   "data": {
                |     "things": [
                |       {
                |         "id": "thing-5",
                |         "name": "THING-5"
                |       },
                |       {
                |         "id": "thing-7",
                |         "name": "THING-7"
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
                |   things(ids: ["thing-6:partition-D", "thing-4:partition-D"]) {
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
                |         "id": "thing-6",
                |         "name": "THING-6"
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
        )

    /**
     * ```json
     * {
     *   "errors": [
     *     {
     *       "message": "Exception while fetching data (/things) : Error fetching things:
     * [thing-1:partition-A, thing-3:partition-A]",
     *       "locations": [
     *         {
     *           "line": 2,
     *           "column": 3
     *         }
     *       ],
     *       "path": [
     *         "things"
     *       ],
     *       "extensions": {
     *         "classification": "DataFetchingException"
     *       }
     *     }
     *   ],
     *   "data": {
     *     "things": [
     *       {
     *         "id": "thing-2",
     *         "name": "THING-2"
     *       },
     *       {
     *         "id": "thing-4",
     *         "name": "THING-4"
     *       },
     *       {
     *         "id": "thing-5",
     *         "name": "THING-5"
     *       },
     *       {
     *         "id": "thing-7",
     *         "name": "THING-7"
     *       },
     *       {
     *         "id": "thing-6",
     *         "name": "THING-6"
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
            |       "message": "Exception while fetching data (/things) : Error fetching things: [thing-1:partition-A, thing-3:partition-A]",
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
            |     "things": [
            |       {
            |         "id": "thing-2",
            |         "name": "THING-2"
            |       },
            |       {
            |         "id": "thing-4",
            |         "name": "THING-4"
            |       },
            |       {
            |         "id": "thing-5",
            |         "name": "THING-5"
            |       },
            |       {
            |         "id": "thing-7",
            |         "name": "THING-7"
            |       },
            |       {
            |         "id": "thing-6",
            |         "name": "THING-6"
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
