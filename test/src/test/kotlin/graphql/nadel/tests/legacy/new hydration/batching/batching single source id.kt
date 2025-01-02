package graphql.nadel.tests.legacy.`new hydration`.batching

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `batching single source id` : NadelLegacyIntegrationTest(
    query = """
        {
          activity {
            content {
              __typename
              ... on Issue {
                id
                title
              }
              ... on Comment {
                id
                content
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
                union ActivityContent = Issue | Comment
                type Activity {
                  id: ID!
                  contentId: ID!
                  content: ActivityContent
                  @hydrated(
                    service: "comments"
                    field: "commentsByIds"
                    arguments: [
                      {name: "ids" value: "${'$'}source.contentId"}
                    ]
                  )
                  @hydrated(
                    service: "issues"
                    field: "issuesByIds"
                    arguments: [
                      {name: "ids" value: "${'$'}source.contentId"}
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
                  contentId: ID!
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("activity") { env ->
                        listOf(
                            Activity_Activity(contentId = "issue/4000"),
                            Activity_Activity(
                                contentId =
                                "issue/8080",
                            ),
                            Activity_Activity(contentId = "issue/7496"),
                            Activity_Activity(contentId = "comment/1234"),
                        )
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
                wiring.type("Query") { type ->
                    val issuesByIds = listOf(
                        Issues_Issue(
                            id = "issue/4000",
                            title = "Four Thousand",
                        ),
                        Issues_Issue(
                            id = "issue/8080",
                            title = "Eighty Eighty",
                        ),
                        Issues_Issue(
                            id = "issue/7496",
                            title = "Seven Four Nine Six",
                        ),
                    ).associateBy { it.id }

                    type.dataFetcher("issuesByIds") { env ->
                        env.getArgument<List<String>>("ids")?.map(issuesByIds::get)
                    }
                }
            },
        ),
        Service(
            name = "comments",
            overallSchema = """
                type Query {
                  commentsByIds(ids: [ID!]!): [Comment!]
                }
                type Comment {
                  id: ID!
                  content: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  commentsByIds(ids: [ID!]!): [Comment!]
                }
                type Comment {
                  id: ID!
                  content: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("commentsByIds") { env ->
                        if (env.getArgument<Any?>("ids") == listOf("comment/1234")) {
                            listOf(Comments_Comment(content = "One Two Three Four", id = "comment/1234"))
                        } else {
                            null
                        }
                    }
                }
            },
        ),
    ),
) {
    private data class Activity_Activity(
        val id: String? = null,
        val contentId: String? = null,
    )

    private data class Issues_Issue(
        val id: String? = null,
        val title: String? = null,
    )

    private data class Comments_Comment(
        val id: String? = null,
        val content: String? = null,
    )
}
