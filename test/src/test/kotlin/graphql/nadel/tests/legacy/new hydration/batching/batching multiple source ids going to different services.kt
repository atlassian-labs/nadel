package graphql.nadel.tests.legacy.`new hydration`.batching

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `batching multiple source ids going to different services` : NadelLegacyIntegrationTest(
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
                  contentIds: [ID!]!
                  content: [ActivityContent]
                  @hydrated(
                    service: "comments"
                    field: "commentsByIds"
                    arguments: [
                      {name: "ids" value: "${'$'}source.contentIds"}
                    ]
                  )
                  @hydrated(
                    service: "issues"
                    field: "issuesByIds"
                    arguments: [
                      {name: "ids" value: "${'$'}source.contentIds"}
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
                  contentIds: [ID!]!
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("activity") { env ->
                        listOf(
                            Activity_Activity(
                                contentIds = listOf(
                                    "issue/4000",
                                    "comment/5000",
                                    "comment/6000",
                                ),
                            ),
                            Activity_Activity(contentIds = listOf("issue/8080")),
                            Activity_Activity(contentIds = listOf("issue/7496", "comment/9001")),
                            Activity_Activity(contentIds = listOf("issue/1234", "comment/1234")),
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
                    val issuesById = listOf(
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
                        Issues_Issue(
                            id = "issue/1234",
                            title = "One Two Three Four",
                        ),
                    ).associateBy { it.id }

                    type.dataFetcher("issuesByIds") { env ->
                        env.getArgument<List<String>>("ids")?.mapNotNull(issuesById::get)
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
                        if (env.getArgument<Any?>("ids") ==
                            listOf(
                                "comment/5000",
                                "comment/6000",
                                "comment/9001",
                                "comment/1234",
                            )
                        ) {
                            listOf(
                                Comments_Comment(content = "Five Thousand", id = "comment/5000"),
                                Comments_Comment(content = "Six Thousand", id = "comment/6000"),
                                Comments_Comment(content = "It's over 9000", id = "comment/9001"),
                                Comments_Comment(content = "One Two Three Four", id = "comment/1234"),
                            )
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
        val contentIds: List<String>? = null,
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
