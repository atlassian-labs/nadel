package graphql.nadel.tests.legacy.`extend type`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `extending types from another service is possible with synthetic fields` : NadelLegacyIntegrationTest(
    query = """
        query {
          root {
            id
            name
            extension {
              id
              name
            }
          }
          anotherRoot
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "Service2",
            overallSchema = """
                type Query {
                  lookUpQuery: LookUpQuery
                }
                type LookUpQuery {
                  lookup(id: ID): Extension
                }
                extend type Root {
                  extension: Extension
                  @hydrated(
                    service: "Service2"
                    field: "lookUpQuery.lookup"
                    arguments: [{name: "id" value: "${'$'}source.id"}]
                    identifiedBy: "id"
                  )
                }
                type Extension {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Extension {
                  id: ID
                  name: String
                }
                type LookUpQuery {
                  lookup(id: ID): Extension
                }
                type Query {
                  lookUpQuery: LookUpQuery
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("lookUpQuery") {
                        Unit
                    }
                }
                wiring.type("LookUpQuery") { type ->
                    type.dataFetcher("lookup") { env ->
                        if (env.getArgument<Any?>("id") == "rootId") {
                            Service2_Extension(id = "rootId", name = "extensionName")
                        } else {
                            null
                        }
                    }
                }
            },
        ),
        Service(
            name = "Service1",
            overallSchema = """
                extend type Query {
                  root: Root
                }
                extend type Query {
                  anotherRoot: String
                }
                type Root {
                  id: ID
                }
                extend type Root {
                  name: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  root: Root
                }
                extend type Query {
                  anotherRoot: String
                }
                type Root {
                  id: ID
                }
                extend type Root {
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type
                        .dataFetcher("anotherRoot") { env ->
                            "anotherRoot"
                        }
                        .dataFetcher("root") { env ->
                            Service1_Root(id = "rootId", name = "rootName")
                        }
                }
            },
        ),
    ),
) {
    private data class Service2_Extension(
        val id: String? = null,
        val name: String? = null,
    )

    private data class Service2_LookUpQuery(
        val lookup: Service2_Extension? = null,
    )

    private data class Service1_Root(
        val id: String? = null,
        val name: String? = null,
    )
}
