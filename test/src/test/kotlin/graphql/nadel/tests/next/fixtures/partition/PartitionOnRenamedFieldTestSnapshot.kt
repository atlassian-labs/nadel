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
    graphql.nadel.tests.next.update<PartitionOnRenamedFieldTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class PartitionOnRenamedFieldTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "things_service",
                query = """
                | query getPartitionedThings {
                |   rename__renamedEcho__echo: echo
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "rename__renamedEcho__echo": "echo"
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
                |   rename__renamedThings__things: things(ids: ["thing-1:partition-A", "thing-3:partition-A"]) {
                |     id
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "rename__renamedThings__things": [
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
                |   rename__renamedThings__things: things(ids: ["thing-2:partition-B", "thing-4:partition-B"]) {
                |     id
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "rename__renamedThings__things": [
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
                |   rename__renamedThings__things: things(ids: ["thing-5:partition-C", "thing-7:partition-C"]) {
                |     id
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "rename__renamedThings__things": [
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
                |   rename__renamedThings__things: things(ids: ["thing-6:partition-D", "thing-8:partition-D"]) {
                |     id
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = " {}",
                result = """
                | {
                |   "data": {
                |     "rename__renamedThings__things": [
                |       {
                |         "id": "thing-6",
                |         "name": "THING-6"
                |       },
                |       {
                |         "id": "thing-8",
                |         "name": "THING-8"
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
     *   "data": {
     *     "renamedEcho": "echo",
     *     "renamedThings": [
     *       {
     *         "id": "thing-1",
     *         "name": "THING-1"
     *       },
     *       {
     *         "id": "thing-3",
     *         "name": "THING-3"
     *       }
     *     ]
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "renamedEcho": "echo",
            |     "renamedThings": [
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
        )
}
