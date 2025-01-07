package graphql.nadel.tests.next.fixtures.hydration.statics

import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionHints
import graphql.nadel.engine.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.next.NadelIntegrationTest

/**
 * Uses hydration to "copy" a field. Does not link two pieces of data together i.e. no $source fields used.
 */
class StaticHydrationAndPolymorphicHydrationTest : NadelIntegrationTest(
    query = """
        query {
          businessReport_findRecentWorkByTeam(teamId: "hello") {
            edges {
              node {
                ... on JiraIssue {
                  key
                }
                ... on BitbucketPullRequest {
                  title
                  patch
                }
              }
              cursor
            }
            pageInfo {
              hasNextPage
            }
          }
        }
    """.trimIndent(),
    services = listOf(
        // Backing service
        Service(
            name = "graph_store",
            overallSchema = """
                type Query {
                  graphStore_query(
                    query: String!
                    first: Int
                    after: String
                  ): GraphStoreQueryConnection
                }
                type GraphStoreQueryConnection {
                  edges: [GraphStoreQueryEdge]
                  pageInfo: PageInfo
                }
                type GraphStoreQueryEdge {
                  nodeId: ID
                  cursor: String
                }
                type PageInfo {
                    hasNextPage: Boolean!
                    hasPreviousPage: Boolean!
                    startCursor: String
                    endCursor: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class GraphStoreQueryEdge(
                    val nodeId: String,
                    val cursor: String?,
                )

                data class PageInfo(
                    val hasNextPage: Boolean,
                    val hasPreviousPage: Boolean,
                    val startCursor: String?,
                    val endCursor: String?,
                )

                data class GraphStoreQueryConnection(
                    val edges: List<GraphStoreQueryEdge>,
                    val pageInfo: PageInfo,
                )

                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("graphStore_query") { env ->
                                GraphStoreQueryConnection(
                                    edges = listOf(
                                        GraphStoreQueryEdge(
                                            nodeId = "ari:cloud:jira::issue/1",
                                            cursor = "1",
                                        ),
                                        GraphStoreQueryEdge(
                                            nodeId = "ari:cloud:bitbucket::pull-request/2",
                                            cursor = "2",
                                        ),
                                    ),
                                    pageInfo = PageInfo(
                                        hasNextPage = true,
                                        hasPreviousPage = false,
                                        startCursor = null,
                                        endCursor = "1",
                                    ),
                                )
                            }
                    }
            },
        ),
        // Service for hydration
        Service(
            name = "jira",
            overallSchema = """
                type Query {
                  issuesByIds(ids: [ID!]!): [JiraIssue]
                }
                type JiraIssue {
                  id: ID!
                  key: String!
                  title: String!
                }
            """.trimIndent(),
            runtimeWiring = { runtime ->
                data class JiraIssue(
                    val id: String,
                    val key: String,
                    val title: String,
                )

                val issuesByIds = listOf(
                    JiraIssue(
                        id = "ari:cloud:jira::issue/1",
                        key = "GQLGW-1",
                        title = "Fix the speed of light",
                    ),
                    JiraIssue(
                        id = "ari:cloud:jira::issue/2",
                        key = "GQLGW-2",
                        title = "Refactor Nadel",
                    ),
                ).strictAssociateBy { it.id }

                runtime
                    .type("Query") { type ->
                        type
                            .dataFetcher("issuesByIds") { env ->
                                val ids = env.getArgument<List<String>>("ids")

                                ids!!
                                    .map {
                                        issuesByIds[it]
                                    }
                            }
                    }
            },
        ),
        Service(
            name = "bitbucket",
            overallSchema = """
                type Query {
                  pullRequestsByIds(ids: [ID!]!): [BitbucketPullRequest]
                }
                type BitbucketPullRequest {
                  id: ID!
                  title: String
                  patch: String
                }
            """.trimIndent(),
            runtimeWiring = { runtime ->
                data class BitbucketPullRequest(
                    val id: String,
                    val title: String,
                    val patch: String,
                )

                val issuesByIds = listOf(
                    BitbucketPullRequest(
                        id = "ari:cloud:bitbucket::pull-request/1",
                        title = "Delete everything",
                        patch = "-",
                    ),
                    BitbucketPullRequest(
                        id = "ari:cloud:bitbucket::pull-request/2",
                        title = "Initial Commit",
                        patch = "+",
                    ),
                ).strictAssociateBy { it.id }

                runtime
                    .type("Query") { type ->
                        type
                            .dataFetcher("pullRequestsByIds") { env ->
                                val ids = env.getArgument<List<String>>("ids")

                                ids!!
                                    .map {
                                        issuesByIds[it]
                                    }
                            }
                    }
            },
        ),
        // Service that introduces virtual type
        Service(
            name = "work",
            overallSchema = """
                type Query {
                  businessReport_findRecentWorkByTeam(
                    teamId: ID!
                    first: Int
                    after: String
                  ): WorkConnection
                    @hydrated(
                      service: "graph_store",
                      field: "graphStore_query"
                      arguments: [
                        {
                          name: "query"
                          value: "SELECT * FROM Work WHERE teamId = ?"
                        }
                        {
                          name: "first"
                          value: "$argument.first"
                        }
                        {
                          name: "after"
                          value: "$argument.after"
                        }
                      ]
                    )
                }
                type WorkConnection @virtualType {
                  edges: [WorkEdge]
                  pageInfo: PageInfo
                }
                type WorkEdge @virtualType {
                  nodeId: ID @hidden
                  node: WorkNode
                    @hydrated(
                      service: "jira"
                      field: "issuesByIds"
                      arguments: [{name: "ids", value: "$source.nodeId"}]
                    )
                    @hydrated(
                      service: "bitbucket"
                      field: "pullRequestsByIds"
                      arguments: [{name: "ids", value: "$source.nodeId"}]
                    )
                  cursor: String
                }
                union WorkNode = JiraIssue | BitbucketPullRequest
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  business_stub: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
            },
        ),
    ),
) {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints()
            .virtualTypeSupport { true }
            .shortCircuitEmptyQuery { true }
    }

    override fun makeNadel(): Nadel.Builder {
        return super.makeNadel()
            .executionHooks(
                object : NadelExecutionHooks {
                    override fun <T : NadelGenericHydrationInstruction> getHydrationInstruction(
                        instructions: List<T>,
                        parentNode: JsonNode,
                        aliasHelper: NadelAliasHelper,
                        userContext: Any?,
                    ): T? {
                        if (instructions.size == 1) {
                            return instructions.single()
                        }

                        @Suppress("UNCHECKED_CAST")
                        val nodeId = (parentNode.value as JsonMap)[aliasHelper.getResultKey("nodeId")] as String

                        val prs = instructions.single { it.backingFieldDef.name == "pullRequestsByIds" }
                        val issues = instructions.single { it.backingFieldDef.name == "issuesByIds" }

                        return when {
                            nodeId.startsWith("ari:cloud:bitbucket::pull-request/") -> prs
                            nodeId.startsWith("ari:cloud:jira::issue/") -> issues
                            else -> throw IllegalArgumentException()
                        }
                    }
                }
            )
    }
}
