package graphql.nadel.tests.legacy.`extend type`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `extending types via hydration with arguments passed on` : NadelLegacyIntegrationTest(
    query = """
        query {
          issue {
            association(filter: {name: "value"}) {
              nameOfAssociation
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "Issue",
            overallSchema = """
                type Query {
                  issue: Issue
                }
                type Issue {
                  id: ID
                }
            """.trimIndent(),
            underlyingSchema = """
                type Issue {
                  id: ID
                }
                type Query {
                  issue: Issue
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("issue") { env ->
                        Issue_Issue(id = "ISSUE-1")
                    }
                }
            },
        ),
        Service(
            name = "Association",
            overallSchema = """
                type Query {
                  association(id: ID, filter: Filter): Association
                }
                input Filter {
                  name: String
                }
                type Association {
                  id: ID
                  nameOfAssociation: String
                }
                extend type Issue {
                  association(filter: Filter): Association
                  @hydrated(
                    service: "Association"
                    field: "association"
                    arguments: [{name: "id" value: "${'$'}source.id"} {name: "filter" value: "${'$'}argument.filter"}]
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Association {
                  id: ID
                  nameOfAssociation: String
                }
                type Query {
                  association(filter: Filter, id: ID): Association
                }
                input Filter {
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("association") { env ->
                        if (env.getArgument<Any?>("filter") == mapOf("name" to "value") &&
                            env.getArgument<Any?>("id") == "ISSUE-1"
                        ) {
                            Association_Association(nameOfAssociation = "ASSOC NAME")
                        } else {
                            null
                        }
                    }
                }
            },
        ),
    ),
) {
    private data class Issue_Issue(
        val id: String? = null,
    )

    private data class Association_Association(
        val id: String? = null,
        val nameOfAssociation: String? = null,
    )

    private data class Association_Filter(
        val name: String? = null,
    )
}
