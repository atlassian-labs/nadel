package graphql.nadel.tests.legacy.`new hydration`.batching

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `batching conditional hydration in abstract type` : NadelLegacyIntegrationTest(
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
            name = "monolith",
            overallSchema = """
                type Query {
                  activity: [IActivity]
                  issuesByIds(ids: [ID!]!): [Issue!]
                  commentsByIds(ids: [ID!]!): [Comment!]
                }
                interface IActivity {
                  content: [ActivityContent]
                }
                union ActivityContent = Issue | Comment
                type Activity implements IActivity {
                  id: ID!
                  content: [ActivityContent]
                  @hydrated(
                    service: "monolith"
                    field: "commentsByIds"
                    arguments: [
                      {name: "ids" value: "${'$'}source.contentIds"}
                    ]
                  )
                  @hydrated(
                    service: "monolith"
                    field: "issuesByIds"
                    arguments: [
                      {name: "ids" value: "${'$'}source.contentIds"}
                    ]
                  )
                }
                type SingleActivity implements IActivity {
                  id: ID!
                  content: [ActivityContent]
                  @hydrated(
                    service: "monolith"
                    field: "issuesByIds"
                    arguments: [
                      {name: "ids" value: "${'$'}source.contentId"}
                    ]
                  )
                  @hydrated(
                    service: "monolith"
                    field: "commentsByIds"
                    arguments: [
                      {name: "ids" value: "$source.contentId"}
                    ]
                  )
                }
                type Issue {
                  id: ID!
                  title: String
                }
                type Comment {
                  id: ID!
                  content: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  activity: [IActivity]
                  commentsByIds(ids: [ID!]!): [Comment!]
                  issuesByIds(ids: [ID!]!): [Issue!]
                }
                interface IActivity {
                  content: [ActivityContent]
                }
                union ActivityContent = Issue | Comment
                type Activity implements IActivity {
                  id: ID!
                  content: [ActivityContent]
                  contentIds: [ID!]
                }
                type SingleActivity implements IActivity {
                  id: ID!
                  content: [ActivityContent]
                  contentId: ID!
                }
                type Issue {
                  id: ID!
                  title: String
                }
                type Comment {
                  id: ID!
                  content: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    val issuesById = listOf(
                        Monolith_Issue(id = "issue/4000", title = "Four Thousand"),
                        Monolith_Issue(id = "issue/7496", title = "Seven Four Nine Six"),
                    ).associateBy { it.id }
                    val commentsById = listOf(
                        Monolith_Comment(content = "Five Thousand", id = "comment/5000"),
                        Monolith_Comment(content = "Six Thousand", id = "comment/6000"),
                        Monolith_Comment(content = "It's over 9000", id = "comment/9001"),
                        Monolith_Comment(content = "One Two Three Four", id = "comment/1234"),
                    ).associateBy { it.id }

                    type
                        .dataFetcher("activity") { env ->
                            listOf(
                                Monolith_Activity(
                                    contentIds = listOf(
                                        "issue/4000",
                                        "comment/5000",
                                        "comment/6000",
                                    ),
                                ),
                                Monolith_SingleActivity(contentId = "issue/8080"),
                                Monolith_Activity(contentIds = listOf("comment/1234", "comment/9001")),
                                Monolith_SingleActivity(contentId = "issue/7496"),
                            )
                        }
                        .dataFetcher("issuesByIds") { env ->
                            env.getArgument<List<String>>("ids")?.mapNotNull(issuesById::get)
                        }
                        .dataFetcher("commentsByIds") { env ->
                            env.getArgument<List<String>>("ids")?.map(commentsById::get)
                        }
                }
                wiring.type("ActivityContent") { type ->
                    type.typeResolver { typeResolver ->
                        val obj = typeResolver.getObject<Any>()
                        val typeName = obj.javaClass.simpleName.substringAfter("_")
                        typeResolver.schema.getTypeAs(typeName)
                    }
                }

                wiring.type("IActivity") { type ->
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
    private data class Monolith_Activity(
        val id: String? = null,
        override val content: List<Monolith_ActivityContent?>? = null,
        val contentIds: List<String>? = null,
    ) : Monolith_IActivity

    private sealed interface Monolith_ActivityContent

    private data class Monolith_Comment(
        val id: String? = null,
        val content: String? = null,
    ) : Monolith_ActivityContent

    private interface Monolith_IActivity {
        val content: List<Monolith_ActivityContent?>?
    }

    private data class Monolith_Issue(
        val id: String? = null,
        val title: String? = null,
    ) : Monolith_ActivityContent

    private data class Monolith_SingleActivity(
        val id: String? = null,
        override val content: List<Monolith_ActivityContent?>? = null,
        val contentId: String? = null,
    ) : Monolith_IActivity
}
