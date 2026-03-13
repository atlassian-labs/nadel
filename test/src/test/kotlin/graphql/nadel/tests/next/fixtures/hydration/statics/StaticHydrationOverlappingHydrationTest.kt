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
import graphql.normalized.ExecutableNormalizedField

/**
 * Tests when the backing type itself declares a hydration.
 * Then the virtual type redefines that hydration.
 *
 * i.e. `GraphStoreQueryEdge.node` defines a hydration and `WorkEdge.node` defines
 * a hydration over it.
 */
class StaticHydrationOverlappingHydrationTest : NadelIntegrationTest(
    query = """
        query {
          graphStore_query(query: "Hello World") {
            edges {
              node {
                __typename
              }
            }
          }
          businessReport_findRecentWorkByTeam(teamId: "hello") {
            __typename
            edges {
              __typename
              node {
                __typename
                ... on JiraIssue {
                  key
                }
              }
              cursor
            }
            pageInfo {
              __typename
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
                  node: GraphStoreQueryNode @idHydrated(idField: "nodeId")
                  cursor: String
                }
                union GraphStoreQueryNode = JiraComment
                type PageInfo {
                    hasNextPage: Boolean!
                    hasPreviousPage: Boolean!
                    startCursor: String
                    endCursor: String
                }
            """.trimIndent(),
            underlyingSchema = """
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
                                            nodeId = "ari:cloud:jira::comment/2",
                                            cursor = "1",
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
                  issues(ids: [ID!]!): [JiraIssue]
                  comments(ids: [ID!]!): [JiraComment]
                }
                type JiraIssue @defaultHydration(field: "issues", idArgument: "ids") {
                  id: ID!
                  key: String!
                  title: String!
                }
                type JiraComment @defaultHydration(field: "comments", idArgument: "ids") {
                  id: ID!
                  text: String!
                }
            """.trimIndent(),
            runtimeWiring = { runtime ->
                data class JiraIssue(
                    val id: String,
                    val key: String,
                    val title: String,
                )

                data class JiraComment(
                    val id: String,
                    val text: String,
                )

                val issuesById = listOf(
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

                val commentsById = listOf(
                    JiraComment(
                        id = "ari:cloud:jira::comment/2",
                        text = "Hello world",
                    ),
                ).strictAssociateBy { it.id }

                runtime
                    .type("Query") { type ->
                        type
                            .dataFetcher("issues") { env ->
                                env.getArgument<List<String>>("ids")!!.map(issuesById::get)
                            }
                            .dataFetcher("comments") { env ->
                                env.getArgument<List<String>>("ids")!!.map(commentsById::get)
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
                  node: WorkNode @idHydrated(idField: "nodeId")
                  cursor: String
                }
                union WorkNode = JiraIssue | JiraComment
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
                        virtualField: ExecutableNormalizedField,
                        instructions: List<T>,
                        sourceInput: JsonNode,
                        userContext: Any?,
                    ): T {
                        throw UnsupportedOperationException()
                    }

                    override fun <T : NadelGenericHydrationInstruction> getHydrationInstruction(
                        virtualField: ExecutableNormalizedField,
                        instructions: List<T>,
                        parentNode: JsonNode,
                        aliasHelper: NadelAliasHelper,
                        userContext: Any?,
                    ): T {
                        return if (instructions.size > 1) {
                            @Suppress("UNCHECKED_CAST")
                            val nodeId = (parentNode.value as JsonMap)[aliasHelper.getResultKey("nodeId")] as String
                            val ati = nodeId.substringBefore("/")
                            when (ati) {
                                "ari:cloud:jira::issue" -> instructions.single { it.backingFieldDef.name == "issues" }
                                "ari:cloud:jira::comment" -> instructions.single { it.backingFieldDef.name == "comments" }
                                else -> throw UnsupportedOperationException()
                            }
                        } else {
                            instructions.single()
                        }
                    }
                }
            )
    }
}
