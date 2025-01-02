package graphql.nadel.tests.legacy.renames.types

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `repeated fragments with renamed types` : NadelLegacyIntegrationTest(
    query = """
        query(${'$'}var1: ID!) {
          service(id: ${'$'}var1) {
            __typename
            dependedOn {
              __typename
              ...ServiceRelationshipsWithGrandchildren
            }
            dependsOn {
              __typename
              ...ServiceRelationshipsWithGrandchildren
            }
            ...BaseServiceInfo
          }
        }
        fragment BaseServiceInfo on MyService {
          __typename
          id
          name
        }
        fragment BaseServiceRelationships on ServiceRelationshipConnection {
          __typename
          nodes {
            __typename
            endService {
              __typename
              ...BaseServiceInfo
            }
            id
            startService {
              __typename
              ...BaseServiceInfo
            }
            type
          }
        }
        fragment ServiceInfoWithChildren on MyService {
          __typename
          dependedOn {
            __typename
            ...BaseServiceRelationships
          }
          dependsOn {
            __typename
            ...BaseServiceRelationships
          }
          ...BaseServiceInfo
        }
        fragment ServiceRelationshipsWithGrandchildren on ServiceRelationshipConnection {
          __typename
          nodes {
            __typename
            endService {
              __typename
              ...ServiceInfoWithChildren
            }
            id
            startService {
              __typename
              ...ServiceInfoWithChildren
            }
            type
          }
        }
    """.trimIndent(),
    variables = mapOf("var1" to "service-1"),
    services = listOf(
        Service(
            name = "Users",
            overallSchema = """
                type Query {
                  service(id: ID!): MyService
                }
                type MyService @renamed(from: "Service") {
                  id: ID!
                  name: String
                  dependedOn: ServiceRelationshipConnection
                  dependsOn: ServiceRelationshipConnection
                }
                type ServiceRelationshipConnection @renamed(from: "RelationshipConnection") {
                  nodes: [ServiceRelationship]
                }
                type ServiceRelationship implements Node @renamed(from: "Relationship") {
                  id: ID!
                  type: String
                  startService: MyService
                  endService: MyService
                }
                interface Node {
                  id: ID!
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  service(id: ID!): Service
                }
                type Service {
                  id: ID!
                  name: String
                  dependedOn: RelationshipConnection
                  dependsOn: RelationshipConnection
                }
                type RelationshipConnection {
                  nodes: [Relationship]
                }
                type Relationship implements Node {
                  id: ID!
                  type: String
                  startService: Service
                  endService: Service
                }
                interface Node {
                  id: ID!
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("service") { env ->
                        if (env.getArgument<Any?>("id") == "service-1") {
                            Users_Service(
                                dependedOn = Users_RelationshipConnection(
                                    nodes = listOf(
                                        Users_Relationship(
                                            endService = null,
                                            id = "relationship-1",
                                            startService = Users_Service(
                                                dependedOn = null,
                                                dependsOn = null,
                                                id = "service-1",
                                                name = "GraphQL Gateway",
                                            ),
                                            type = "unsure",
                                        ),
                                    ),
                                ),
                                dependsOn = Users_RelationshipConnection(nodes = listOf()),
                                id = "service-0",
                                name = "API Gateway",
                            )
                        } else {
                            null
                        }
                    }
                }
                wiring.type("Node") { type ->
                    type.typeResolver { typeResolver ->
                        val obj = typeResolver.getObject<Any>()
                        val typeName = obj.javaClass.simpleName.substringAfter("_")
                        typeResolver.schema.getTypeAs(typeName)
                    }
                }
            },
        ),
    ),
) {
    private interface Users_Node {
        val id: String?
    }

    private data class Users_Relationship(
        override val id: String? = null,
        val type: String? = null,
        val startService: Users_Service? = null,
        val endService: Users_Service? = null,
    ) : Users_Node

    private data class Users_RelationshipConnection(
        val nodes: List<Users_Relationship?>? = null,
    )

    private data class Users_Service(
        val id: String? = null,
        val name: String? = null,
        val dependedOn: Users_RelationshipConnection? = null,
        val dependsOn: Users_RelationshipConnection? = null,
    )
}
