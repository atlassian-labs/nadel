package graphql.nadel.tests.legacy.`new hydration`.batching

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `batching null source input object` : NadelLegacyIntegrationTest(
    query = """
        {
          activity {
            content {
              __typename
              ... on Issue {
                id
                title
              }
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "activity",
            overallSchema = """
                type Query {
                  activity: [Activity]
                }
                type Activity {
                  id: ID!
                  content: Issue
                    @hydrated(
                      service: "issues"
                      field: "issuesByIds"
                      arguments: [
                        {name: "ids" value: "${'$'}source.reference.issueId"}
                      ]
                    )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  activity: [Activity]
                }
                type Activity {
                  id: ID!
                  reference: ActivityReference
                }
                type ActivityReference {
                  issueId: ID
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("activity") { env ->
                        listOf(Activity_Activity(reference = null))
                    }
                }
            },
        ),
        Service(
            name = "issues",
            overallSchema = """
                type Query {
                  issuesByIds(ids: [ID!]!): [Issue!]
                }
                type Issue {
                  id: ID!
                  title: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  issuesByIds(ids: [ID!]!): [Issue!]
                }
                type Issue {
                  id: ID!
                  title: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
            },
        ),
    ),
) {
    private data class Activity_Activity(
        val id: String? = null,
        val reference: Activity_ActivityReference? = null,
    )

    private data class Activity_ActivityReference(
        val issueId: String? = null,
    )

    private data class Issues_Issue(
        val id: String? = null,
        val title: String? = null,
    )
}
