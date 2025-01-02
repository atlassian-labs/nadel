package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `hydration with interfaces asking typename` : NadelLegacyIntegrationTest(
    query = """
        query {
          nodes {
            __typename
            id
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "Issues",
            overallSchema = """
                type Query {
                  nodes: [Node]
                  ariById(id: ID!): ID
                }
                type JiraIssue implements Node @renamed(from: "Issue") {
                  id: ID
                }
                interface Node {
                  id: ID
                }
                type User implements Node {
                  id: ID
                  @hydrated(
                    service: "Issues"
                    field: "ariById"
                    arguments: [{name: "id" value: "${'$'}source.id"}]
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  ariById(id: ID!): ID
                  nodes: [Node]
                }
                interface Node {
                  id: ID
                }
                type Issue implements Node {
                  id: ID
                }
                type User implements Node {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type
                        .dataFetcher("nodes") { env ->
                            listOf(
                                Issues_Issue(id = "GQLGW-001"),
                                Issues_Issue(id = "GQLGW-1102"),
                                Issues_User(
                                    id = "ari:i-always-forget-the-format/1",
                                ),
                            )
                        }
                        .dataFetcher("ariById") { env ->
                            if (env.getArgument<Any?>("id") == "ari:i-always-forget-the-format/1") {
                                "Definitely an ARI"
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
    private data class Issues_Issue(
        override val id: String? = null,
    ) : Issues_Node

    private interface Issues_Node {
        val id: String?
    }

    private data class Issues_User(
        override val id: String? = null,
        val name: String? = null,
    ) : Issues_Node
}
