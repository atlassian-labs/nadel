package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `hydration with interfaces no source id` : NadelLegacyIntegrationTest(
    query = """
        query {
          nodes {
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
                  idByAri(id: ID!): ID
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
                    field: "idByAri"
                    arguments: [{name: "id" value: "${'$'}source.ari"}]
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  nodes: [Node]
                  idByAri(id: ID!): ID
                }
                interface Node {
                  id: ID
                }
                type Issue implements Node {
                  id: ID
                }
                type User implements Node {
                  id: ID
                  ari: ID!
                  name: String!
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("nodes") { env ->
                        listOf(Issues_Issue(id = "GQLGW-001"), Issues_Issue(id = "GQLGW-1102"))
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
        val ari: String? = null,
        val name: String? = null,
    ) : Issues_Node
}
