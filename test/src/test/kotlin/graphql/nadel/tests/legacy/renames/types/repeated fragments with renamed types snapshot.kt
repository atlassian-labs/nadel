// @formatter:off
package graphql.nadel.tests.legacy.renames.types

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`repeated fragments with renamed types`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `repeated fragments with renamed types snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Users",
                query = """
                | query {
                |   service(id: "service-1") {
                |     __typename
                |     dependedOn {
                |       __typename
                |       nodes {
                |         __typename
                |         endService {
                |           __typename
                |           dependedOn {
                |             __typename
                |             nodes {
                |               __typename
                |               endService {
                |                 __typename
                |                 id
                |                 name
                |               }
                |               id
                |               startService {
                |                 __typename
                |                 id
                |                 name
                |               }
                |               type
                |             }
                |           }
                |           dependsOn {
                |             __typename
                |             nodes {
                |               __typename
                |               endService {
                |                 __typename
                |                 id
                |                 name
                |               }
                |               id
                |               startService {
                |                 __typename
                |                 id
                |                 name
                |               }
                |               type
                |             }
                |           }
                |           id
                |           name
                |         }
                |         id
                |         startService {
                |           __typename
                |           dependedOn {
                |             __typename
                |             nodes {
                |               __typename
                |               endService {
                |                 __typename
                |                 id
                |                 name
                |               }
                |               id
                |               startService {
                |                 __typename
                |                 id
                |                 name
                |               }
                |               type
                |             }
                |           }
                |           dependsOn {
                |             __typename
                |             nodes {
                |               __typename
                |               endService {
                |                 __typename
                |                 id
                |                 name
                |               }
                |               id
                |               startService {
                |                 __typename
                |                 id
                |                 name
                |               }
                |               type
                |             }
                |           }
                |           id
                |           name
                |         }
                |         type
                |       }
                |     }
                |     dependsOn {
                |       __typename
                |       nodes {
                |         __typename
                |         endService {
                |           __typename
                |           dependedOn {
                |             __typename
                |             nodes {
                |               __typename
                |               endService {
                |                 __typename
                |                 id
                |                 name
                |               }
                |               id
                |               startService {
                |                 __typename
                |                 id
                |                 name
                |               }
                |               type
                |             }
                |           }
                |           dependsOn {
                |             __typename
                |             nodes {
                |               __typename
                |               endService {
                |                 __typename
                |                 id
                |                 name
                |               }
                |               id
                |               startService {
                |                 __typename
                |                 id
                |                 name
                |               }
                |               type
                |             }
                |           }
                |           id
                |           name
                |         }
                |         id
                |         startService {
                |           __typename
                |           dependedOn {
                |             __typename
                |             nodes {
                |               __typename
                |               endService {
                |                 __typename
                |                 id
                |                 name
                |               }
                |               id
                |               startService {
                |                 __typename
                |                 id
                |                 name
                |               }
                |               type
                |             }
                |           }
                |           dependsOn {
                |             __typename
                |             nodes {
                |               __typename
                |               endService {
                |                 __typename
                |                 id
                |                 name
                |               }
                |               id
                |               startService {
                |                 __typename
                |                 id
                |                 name
                |               }
                |               type
                |             }
                |           }
                |           id
                |           name
                |         }
                |         type
                |       }
                |     }
                |     id
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "service": {
                |       "__typename": "Service",
                |       "id": "service-0",
                |       "name": "API Gateway",
                |       "dependedOn": {
                |         "__typename": "RelationshipConnection",
                |         "nodes": [
                |           {
                |             "__typename": "Relationship",
                |             "id": "relationship-1",
                |             "type": "unsure",
                |             "endService": null,
                |             "startService": {
                |               "__typename": "Service",
                |               "id": "service-1",
                |               "name": "GraphQL Gateway",
                |               "dependsOn": null,
                |               "dependedOn": null
                |             }
                |           }
                |         ]
                |       },
                |       "dependsOn": {
                |         "__typename": "RelationshipConnection",
                |         "nodes": []
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
     *     "service": {
     *       "__typename": "MyService",
     *       "id": "service-0",
     *       "name": "API Gateway",
     *       "dependedOn": {
     *         "__typename": "ServiceRelationshipConnection",
     *         "nodes": [
     *           {
     *             "__typename": "ServiceRelationship",
     *             "id": "relationship-1",
     *             "type": "unsure",
     *             "endService": null,
     *             "startService": {
     *               "__typename": "MyService",
     *               "id": "service-1",
     *               "name": "GraphQL Gateway",
     *               "dependsOn": null,
     *               "dependedOn": null
     *             }
     *           }
     *         ]
     *       },
     *       "dependsOn": {
     *         "__typename": "ServiceRelationshipConnection",
     *         "nodes": []
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
            |     "service": {
            |       "__typename": "MyService",
            |       "id": "service-0",
            |       "name": "API Gateway",
            |       "dependedOn": {
            |         "__typename": "ServiceRelationshipConnection",
            |         "nodes": [
            |           {
            |             "__typename": "ServiceRelationship",
            |             "id": "relationship-1",
            |             "type": "unsure",
            |             "endService": null,
            |             "startService": {
            |               "__typename": "MyService",
            |               "id": "service-1",
            |               "name": "GraphQL Gateway",
            |               "dependsOn": null,
            |               "dependedOn": null
            |             }
            |           }
            |         ]
            |       },
            |       "dependsOn": {
            |         "__typename": "ServiceRelationshipConnection",
            |         "nodes": []
            |       }
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
