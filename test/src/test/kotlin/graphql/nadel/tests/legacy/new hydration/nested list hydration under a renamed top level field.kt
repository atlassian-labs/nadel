package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `nested list hydration under a renamed top level field` : NadelLegacyIntegrationTest(
    query = """
        query {
          fooService {
            otherServices {
              nodes {
                space {
                  id
                }
              }
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "Foo",
            overallSchema = """
                type Query {
                  fooService: FooService @renamed(from: "service")
                  connection(id: ID): Connection
                  node(id: ID): Node
                  space(id: ID): Space
                }
                type FooService @renamed(from: "Service") {
                  otherServices: Connection
                  @hydrated(
                    service: "Foo"
                    field: "connection"
                    arguments: [{name: "id" value: "${'$'}source.id"}]
                  )
                }
                type Connection {
                  edges: [Edge]
                  nodes: [Node]
                  @hydrated(
                    service: "Foo"
                    field: "node"
                    arguments: [{name: "id" value: "${'$'}source.edges.node"}]
                  )
                }
                type Node {
                  space: Space
                  @hydrated(
                    service: "Foo"
                    field: "space"
                    arguments: [{name: "id" value: "${'$'}source.id"}]
                  )
                  id: ID
                }
                type Space {
                  id: ID
                  name: String
                }
                type Edge {
                  node: Node
                  @hydrated(
                    service: "Foo"
                    field: "node"
                    arguments: [{name: "id" value: "${'$'}source.node"}]
                  )
                  name: String
                  id: ID
                }
            """.trimIndent(),
            underlyingSchema = """
                type Connection {
                  edges: [Edge]
                  nodes: [ID]
                }
                type Edge {
                  id: ID
                  name: String
                  node: ID
                }
                type Node {
                  detailId: ID!
                  id: ID
                  name: String
                }
                type Query {
                  connection(id: ID): Connection
                  node(id: ID): Node
                  service: Service
                  space(id: ID): Space
                }
                type Service {
                  id: ID
                }
                type Space {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type
                        .dataFetcher("service") { env ->
                            Foo_Service(id = "ID")
                        }
                        .dataFetcher("connection") { env ->
                            if (env.getArgument<Any?>("id") == "ID") {
                                Foo_Connection(edges = listOf(Foo_Edge(node = "1")))
                            } else {
                                null
                            }
                        }
                        .dataFetcher("node") { env ->
                            if (env.getArgument<Any?>("id") == "1") {
                                Foo_Node(id = "1a")
                            } else {
                                null
                            }
                        }
                        .dataFetcher("space") { env ->
                            if (env.getArgument<Any?>("id") == "1a") {
                                Foo_Space(id = "apple")
                            } else {
                                null
                            }
                        }
                }
            },
        ),
    ),
) {
    private data class Foo_Connection(
        val edges: List<Foo_Edge?>? = null,
        val nodes: List<String?>? = null,
    )

    private data class Foo_Edge(
        val id: String? = null,
        val name: String? = null,
        val node: String? = null,
    )

    private data class Foo_Node(
        val detailId: String? = null,
        val id: String? = null,
        val name: String? = null,
    )

    private data class Foo_Service(
        val id: String? = null,
    )

    private data class Foo_Space(
        val id: String? = null,
        val name: String? = null,
    )
}
