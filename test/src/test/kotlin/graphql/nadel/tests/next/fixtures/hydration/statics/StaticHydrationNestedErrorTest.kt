package graphql.nadel.tests.next.fixtures.hydration.statics

import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionHints
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformServiceExecutionContext
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.error.NadelGraphQLErrorException
import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.normalized.ExecutableNormalizedField

class StaticHydrationNestedErrorTest : NadelIntegrationTest(
    query = """
        query {
          businessReport_findRecentWorkByTeam(teamId: "hello") {
            __typename
            edges {
              __typename
              node {
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
                  node: WorkNode
                    @hydrated(
                      field: "issuesByIds"
                      arguments: [{name: "ids", value: "$source.nodeId"}]
                    )
                  cursor: String
                }
                union WorkNode = JiraIssue
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
            .transforms(
                listOf(
                    ThrowErrorTransform(),
                ),
            )
    }

    private class ThrowErrorTransform : NadelTransform<Any> {
        override suspend fun isApplicable(
            executionContext: NadelExecutionContext,
            serviceExecutionContext: NadelServiceExecutionContext,
            executionBlueprint: NadelOverallExecutionBlueprint,
            services: Map<String, graphql.nadel.Service>,
            service: graphql.nadel.Service,
            overallField: ExecutableNormalizedField,
            transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
            hydrationDetails: ServiceExecutionHydrationDetails?,
        ): Any? {
            if (overallField.name == "issuesByIds") {
                throw Bye()
            }

            return null
        }

        override suspend fun transformField(
            executionContext: NadelExecutionContext,
            serviceExecutionContext: NadelServiceExecutionContext,
            transformer: NadelQueryTransformer,
            executionBlueprint: NadelOverallExecutionBlueprint,
            service: graphql.nadel.Service,
            field: ExecutableNormalizedField,
            state: Any,
            transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
        ): NadelTransformFieldResult {
            return NadelTransformFieldResult.unmodified(field)
        }

        override suspend fun getResultInstructions(
            executionContext: NadelExecutionContext,
            serviceExecutionContext: NadelServiceExecutionContext,
            executionBlueprint: NadelOverallExecutionBlueprint,
            service: graphql.nadel.Service,
            overallField: ExecutableNormalizedField,
            underlyingParentField: ExecutableNormalizedField?,
            result: ServiceExecutionResult,
            state: Any,
            nodes: JsonNodes,
            transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
        ): List<NadelResultInstruction> {
            return emptyList()
        }
    }

    class Bye : NadelGraphQLErrorException(
        message = "BYE",
    )
}
