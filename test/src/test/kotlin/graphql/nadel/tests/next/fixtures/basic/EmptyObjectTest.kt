package graphql.nadel.tests.next.fixtures.basic

import graphql.nadel.tests.next.NadelIntegrationTest

class EmptyObjectTest : NadelIntegrationTest(
    query = """
        query {
          node(id: "issue") {
            ... on Project {
              id
            }
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "myService",
            overallSchema = """
                type Query {
                    node(id: ID!): Node
                }
                interface Node {
                  id: ID!
                }
                type Issue implements Node {
                  id: ID!
                }
                type Project implements Node {
                  id: ID!
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Issue(val id: String)
                data class Project(val id: String)
                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("node") { env ->
                                Issue("wow")
                            }
                    }
                    .type("Node") {
                        it.typeResolver { env ->
                            env.schema.getObjectType(env.getObject<Any?>().javaClass.simpleName)
                        }
                    }
            },
        ),
    ),
)
