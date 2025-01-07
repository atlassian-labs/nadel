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
                | {
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
                |       "dependedOn": {
                |         "__typename": "RelationshipConnection",
                |         "nodes": [
                |           {
                |             "__typename": "Relationship",
                |             "endService": null,
                |             "id": "relationship-1",
                |             "startService": {
                |               "__typename": "Service",
                |               "dependedOn": null,
                |               "dependsOn": null,
                |               "id": "service-1",
                |               "name": "GraphQL Gateway"
                |             },
                |             "type": "unsure"
                |           }
                |         ]
                |       },
                |       "dependsOn": {
                |         "__typename": "RelationshipConnection",
                |         "nodes": []
                |       },
                |       "id": "service-0",
                |       "name": "API Gateway"
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
     *       "dependedOn": {
     *         "__typename": "ServiceRelationshipConnection",
     *         "nodes": [
     *           {
     *             "__typename": "ServiceRelationship",
     *             "endService": null,
     *             "id": "relationship-1",
     *             "startService": {
     *               "__typename": "MyService",
     *               "dependedOn": null,
     *               "dependsOn": null,
     *               "id": "service-1",
     *               "name": "GraphQL Gateway"
     *             },
     *             "type": "unsure"
     *           }
     *         ]
     *       },
     *       "dependsOn": {
     *         "__typename": "ServiceRelationshipConnection",
     *         "nodes": []
     *       },
     *       "id": "service-0",
     *       "name": "API Gateway"
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
            |       "dependedOn": {
            |         "__typename": "ServiceRelationshipConnection",
            |         "nodes": [
            |           {
            |             "__typename": "ServiceRelationship",
            |             "endService": null,
            |             "id": "relationship-1",
            |             "startService": {
            |               "__typename": "MyService",
            |               "dependedOn": null,
            |               "dependsOn": null,
            |               "id": "service-1",
            |               "name": "GraphQL Gateway"
            |             },
            |             "type": "unsure"
            |           }
            |         ]
            |       },
            |       "dependsOn": {
            |         "__typename": "ServiceRelationshipConnection",
            |         "nodes": []
            |       },
            |       "id": "service-0",
            |       "name": "API Gateway"
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
