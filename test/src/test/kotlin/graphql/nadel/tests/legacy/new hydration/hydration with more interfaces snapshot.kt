// @formatter:off
package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`hydration with more interfaces`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `hydration with more interfaces snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Issues",
                query = """
                | {
                |   ariById(id: "Franklin")
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "ariById": "ari:user/Franklin"
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Issues",
                query = """
                | {
                |   nodes {
                |     ... on Issue {
                |       id
                |     }
                |     ... on Troll {
                |       __typename__hydration__id: __typename
                |       hydration__id__id: id
                |     }
                |     ... on User {
                |       __typename__hydration__id: __typename
                |       hydration__id__id: id
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "nodes": [
                |       {
                |         "id": "GQLGW-001"
                |       },
                |       {
                |         "id": "GQLGW-1102"
                |       },
                |       {
                |         "hydration__id__id": "My Arm",
                |         "__typename__hydration__id": "Troll"
                |       },
                |       {
                |         "hydration__id__id": "Franklin",
                |         "__typename__hydration__id": "User"
                |       },
                |       {
                |         "id": "GQLGW-11"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Issues",
                query = """
                | {
                |   trollName(id: "My Arm")
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "trollName": "Troll"
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
     *     "nodes": [
     *       {
     *         "id": "GQLGW-001"
     *       },
     *       {
     *         "id": "GQLGW-1102"
     *       },
     *       {
     *         "id": "Troll"
     *       },
     *       {
     *         "id": "ari:user/Franklin"
     *       },
     *       {
     *         "id": "GQLGW-11"
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
            |     "nodes": [
            |       {
            |         "id": "GQLGW-001"
            |       },
            |       {
            |         "id": "GQLGW-1102"
            |       },
            |       {
            |         "id": "Troll"
            |       },
            |       {
            |         "id": "ari:user/Franklin"
            |       },
            |       {
            |         "id": "GQLGW-11"
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
