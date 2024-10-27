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
    graphql.nadel.tests.next.update<PartitionTypeWithMultipleRoutingFieldsTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class PartitionTypeWithMultipleRoutingFieldsTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "things_service",
                query = """
                | query getPartitionedThings {
                |   things(filter: {thingsIds : [{primaryId : "thing-1-primary:partition-A", secondaryId : "thing-1-secondary"}, {primaryId : "thing-3-primary-no-partition", secondaryId : "thing-3-secondary:partition-A"}]}) {
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
                |         "id": "thing-1-primary",
                |         "name": "THING-1-PRIMARY"
                |       },
                |       {
                |         "id": "thing-3-primary-no-partition",
                |         "name": "THING-3-PRIMARY-NO-PARTITION"
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
                |   things(filter: {thingsIds : [{primaryId : "thing-2-same-partition:partition-B", secondaryId : "thing-2-secondary-same-partition:partition-B"}, {primaryId : "thing-4-primary:partition-B", secondaryId : "thing-4-secondary"}]}) {
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
                |         "id": "thing-2-same-partition",
                |         "name": "THING-2-SAME-PARTITION"
                |       },
                |       {
                |         "id": "thing-4-primary",
                |         "name": "THING-4-PRIMARY"
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
     *     "things": [
     *       {
     *         "id": "thing-1-primary",
     *         "name": "THING-1-PRIMARY"
     *       },
     *       {
     *         "id": "thing-3-primary-no-partition",
     *         "name": "THING-3-PRIMARY-NO-PARTITION"
     *       },
     *       {
     *         "id": "thing-2-same-partition",
     *         "name": "THING-2-SAME-PARTITION"
     *       },
     *       {
     *         "id": "thing-4-primary",
     *         "name": "THING-4-PRIMARY"
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
            |     "things": [
            |       {
            |         "id": "thing-1-primary",
            |         "name": "THING-1-PRIMARY"
            |       },
            |       {
            |         "id": "thing-3-primary-no-partition",
            |         "name": "THING-3-PRIMARY-NO-PARTITION"
            |       },
            |       {
            |         "id": "thing-2-same-partition",
            |         "name": "THING-2-SAME-PARTITION"
            |       },
            |       {
            |         "id": "thing-4-primary",
            |         "name": "THING-4-PRIMARY"
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
