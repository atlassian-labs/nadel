package graphql.nadel.tests.legacy.`new hydration`.batching

import graphql.language.AstPrinter
import graphql.language.AstSorter
import graphql.nadel.NadelServiceExecutionResultImpl
import graphql.nadel.ServiceExecution
import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import java.security.MessageDigest
import java.util.concurrent.CompletableFuture
import kotlin.test.assertTrue

class `batching absent source input` : NadelLegacyIntegrationTest(
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
                  contentId: ID
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
                  contentId: ID
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("activity") { env ->
                        listOf(
                            Activity_Activity(contentId = null),
                            Activity_Activity(contentId = ""),
                            Activity_Activity(contentId = "comment/9001"),
                            Activity_Activity(
                                contentId = "issue/1234",
                            ),
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
                            id = "issue/1234",
                            title = "One Two Three Four",
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
                        if (env.getArgument<Any?>("ids") == listOf("comment/9001")) {
                            listOf(Comments_Comment(content = "It's over 9000", id = "comment/9001"))
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

    override fun makeServiceExecution(service: Service): ServiceExecution {
        if (service.name == "activity") {
            // This test returns data that normal GraphQL can't i.e. it's illegal response missing fields
            return ServiceExecution {
                val query = AstPrinter.printAstCompact(AstSorter().sort(it.query))

                @OptIn(ExperimentalStdlibApi::class)
                val queryHash = MessageDigest
                    .getInstance("SHA-1")
                    .digest(query.toByteArray(Charsets.UTF_8))
                    .toHexString()

                assertTrue(queryHash == "c123547d3405f8b721b5ed0802570f034cfaa9a7")

                CompletableFuture.completedFuture(
                    NadelServiceExecutionResultImpl(
                        data = mutableMapOf(
                            "activity" to mutableListOf(
                                mutableMapOf(
                                    "__typename__batch_hydration__content" to "Activity",
                                ),
                                mutableMapOf(
                                    "__typename__batch_hydration__content" to "Activity",
                                    "batch_hydration__content__contentId" to "",
                                ),
                                mutableMapOf(
                                    "__typename__batch_hydration__content" to "Activity",
                                    "batch_hydration__content__contentId" to "comment/9001",
                                ),
                                mutableMapOf(
                                    "__typename__batch_hydration__content" to "Activity",
                                    "batch_hydration__content__contentId" to "issue/1234",
                                ),
                            ),
                        ),
                    ),
                )
            }
        }

        return super.makeServiceExecution(service)
    }
}
