package graphql.nadel.tests.legacy.renames.types

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `renamed type is shared even deeper` : NadelLegacyIntegrationTest(
    query = """
        query {
          elements {
            __typename
            nodes {
              __typename
              other {
                __typename
                id
              }
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "Nextgen",
            overallSchema = """
                type Query {
                  elements: ElementConnection
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  elements: ElementConnection
                }
                type ElementConnection {
                  nodes: [Element]
                }
                type Element implements Node {
                  id: ID
                  other: Other
                }
                type Other {
                  id: ID!
                }
                interface Node {
                  id: ID
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("elements") { env ->
                        Nextgen_ElementConnection(
                            nodes = listOf(
                                Nextgen_Element(
                                    other = Nextgen_Other(
                                        id = "OTHER-1",
                                    ),
                                ),
                            ),
                        )
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
        Service(
            name = "Service",
            overallSchema = """
                type ElementConnection {
                  nodes: [Element]
                }
                type Element implements Node {
                  id: ID
                  other: RenamedOther
                }
                type RenamedOther @renamed(from: "Other") {
                  id: ID!
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  echo: String
                }
                type ElementConnection {
                  nodes: [Element]
                }
                type Element implements Node {
                  id: ID
                  other: Other
                }
                type Other {
                  id: ID!
                }
                interface Node {
                  id: ID
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Node") { type ->
                    type.typeResolver { typeResolver ->
                        val obj = typeResolver.getObject<Any>()
                        val typeName = obj.javaClass.simpleName.substringAfter("_")
                        typeResolver.schema.getTypeAs(typeName)
                    }
                }
            },
        ),
        Service(
            name = "Shared",
            overallSchema = """
                interface Node {
                  id: ID
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  node(id: ID): Node
                }
                interface Node {
                  id: ID
                }
                type Stub implements Node {
                  id: ID
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
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
    private data class Nextgen_Element(
        override val id: String? = null,
        val other: Nextgen_Other? = null,
    ) : Nextgen_Node

    private data class Nextgen_ElementConnection(
        val nodes: List<Nextgen_Element?>? = null,
    )

    private interface Nextgen_Node {
        val id: String?
    }

    private data class Nextgen_Other(
        val id: String? = null,
    )

    private data class Service_Element(
        override val id: String? = null,
        val other: Service_Other? = null,
    ) : Service_Node

    private data class Service_ElementConnection(
        val nodes: List<Service_Element?>? = null,
    )

    private interface Service_Node {
        val id: String?
    }

    private data class Service_Other(
        val id: String? = null,
    )

    private interface Shared_Node {
        val id: String?
    }

    private data class Shared_Stub(
        override val id: String? = null,
    ) : Shared_Node
}
