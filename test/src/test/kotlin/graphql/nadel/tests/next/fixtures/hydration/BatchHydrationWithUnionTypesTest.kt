package graphql.nadel.tests.next.fixtures.hydration

import graphql.ExecutionResult
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.tests.next.NadelIntegrationTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests batch hydration with union types where only some union members need hydration.
 *
 * This test verifies the customer scenario where:
 * - A field returns a union type: `AgentStudioToolType = AgentStudioTool | AgentStudioMcpTool | AgentStudioMcpServer`
 * - Some union members need hydration (AgentStudioMcpTool, AgentStudioMcpServer)
 * - Some union members don't need hydration (AgentStudioTool)
 *
 * The question is: Will Nadel be smart enough to:
 * 1. Filter items by their concrete type
 * 2. Batch hydrate all AgentStudioMcpTool items together
 * 3. Batch hydrate all AgentStudioMcpServer items together
 * 4. Skip hydration for AgentStudioTool items
 *
 * ## Key Verification
 * The test verifies that:
 * - ONE batched call is made for all MCP tools: `mcpToolsByIds(ids: ["mcp-tool-1", "mcp-tool-2"])`
 * - ONE batched call is made for all MCP servers: `mcpServersByIds(ids: ["mcp-server-1", "mcp-server-2"])`
 * - No hydration calls are made for regular tools
 */
class BatchHydrationWithUnionTypesTest : NadelIntegrationTest(
    query = """
        query {
          scenario(id: "scenario-1") {
            id
            name
            tools {
              ... on AgentStudioTool {
                id
                displayName
                integrationKey
              }
              ... on AgentStudioMcpTool {
                id
                mcpTool {
                  id
                  name
                  description
                }
              }
              ... on AgentStudioMcpServer {
                id
                mcpServer {
                  id
                  name
                  url
                }
              }
            }
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "agent_studio",
            overallSchema = """
                type Query {
                  scenario(id: ID!): AgentStudioScenario
                }
                
                interface AgentStudioScenario {
                  id: ID!
                  name: String!
                  tools: [AgentStudioToolType!]!
                }
                
                type AgentStudioScenarioImpl implements AgentStudioScenario {
                  id: ID!
                  name: String!
                  tools: [AgentStudioToolType!]!
                }
                
                union AgentStudioToolType = AgentStudioTool | AgentStudioMcpTool | AgentStudioMcpServer
                
                type AgentStudioTool {
                  id: ID!
                  iconUrl: String
                  displayName: String
                  description: String
                  integrationKey: String
                  definitionId: String
                  tags: [String!]
                }
                
                type AgentStudioMcpTool {
                  id: ID!
                  mcpTool: IntegrationServiceMcpTool @idHydrated(idField: "id")
                }
                
                type AgentStudioMcpServer {
                  id: ID!
                  mcpServer: IntegrationServiceMcpServer @idHydrated(idField: "id")
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class AgentStudioTool(
                    val id: String,
                    val iconUrl: String? = null,
                    val displayName: String,
                    val description: String? = null,
                    val integrationKey: String,
                    val definitionId: String? = null,
                    val tags: List<String>? = null,
                )

                data class AgentStudioMcpTool(
                    val id: String,
                )

                data class AgentStudioMcpServer(
                    val id: String,
                )

                data class AgentStudioScenarioImpl(
                    val id: String,
                    val name: String,
                    val tools: List<Any>, // Mix of different types
                )

                val scenario = AgentStudioScenarioImpl(
                    id = "scenario-1",
                    name = "My Test Scenario",
                    tools = listOf(
                        // Regular tool (no hydration)
                        AgentStudioTool(
                            id = "tool-1",
                            displayName = "Regular Tool 1",
                            integrationKey = "regular-tool",
                        ),
                        // MCP tool (needs hydration)
                        AgentStudioMcpTool(
                            id = "mcp-tool-1",
                        ),
                        // Another regular tool (no hydration)
                        AgentStudioTool(
                            id = "tool-2",
                            displayName = "Regular Tool 2",
                            integrationKey = "another-regular-tool",
                        ),
                        // Another MCP tool (needs hydration - should batch with first one)
                        AgentStudioMcpTool(
                            id = "mcp-tool-2",
                        ),
                        // MCP server (needs hydration, different type)
                        AgentStudioMcpServer(
                            id = "mcp-server-1",
                        ),
                        // Another MCP server (needs hydration - should batch with first one)
                        AgentStudioMcpServer(
                            id = "mcp-server-2",
                        ),
                    )
                )

                wiring
                    .type("Query") { type ->
                        type.dataFetcher("scenario") { env ->
                            scenario
                        }
                    }
                    .type("AgentStudioScenario") { type ->
                        type.typeResolver { env ->
                            env.schema.getObjectType("AgentStudioScenarioImpl")
                        }
                    }
                    .type("AgentStudioToolType") { type ->
                        type.typeResolver { env ->
                            val obj = env.getObject<Any>()
                            when (obj) {
                                is AgentStudioTool -> env.schema.getObjectType("AgentStudioTool")
                                is AgentStudioMcpTool -> env.schema.getObjectType("AgentStudioMcpTool")
                                is AgentStudioMcpServer -> env.schema.getObjectType("AgentStudioMcpServer")
                                else -> null
                            }
                        }
                    }
            }
        ),
        Service(
            name = "integration_service",
            overallSchema = """
                type Query {
                  mcpToolsByIds(ids: [ID!]!): [IntegrationServiceMcpTool]
                  mcpServersByIds(ids: [ID!]!): [IntegrationServiceMcpServer]
                }
                
                type IntegrationServiceMcpTool @defaultHydration(
                  field: "mcpToolsByIds"
                  idArgument: "ids"
                  batchSize: 100
                  identifiedBy: "id"
                ) {
                  id: ID!
                  name: String!
                  description: String
                }
                
                type IntegrationServiceMcpServer @defaultHydration(
                  field: "mcpServersByIds"
                  idArgument: "ids"
                  batchSize: 100
                  identifiedBy: "id"
                ) {
                  id: ID!
                  name: String!
                  url: String!
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class IntegrationServiceMcpTool(
                    val id: String,
                    val name: String,
                    val description: String? = null,
                )

                data class IntegrationServiceMcpServer(
                    val id: String,
                    val name: String,
                    val url: String,
                )

                val mcpTools = listOf(
                    IntegrationServiceMcpTool(
                        id = "mcp-tool-1",
                        name = "MCP Tool Alpha",
                        description = "First MCP tool",
                    ),
                    IntegrationServiceMcpTool(
                        id = "mcp-tool-2",
                        name = "MCP Tool Beta",
                        description = "Second MCP tool",
                    ),
                )
                val mcpToolsById = mcpTools.strictAssociateBy { it.id }

                val mcpServers = listOf(
                    IntegrationServiceMcpServer(
                        id = "mcp-server-1",
                        name = "MCP Server One",
                        url = "https://server1.example.com",
                    ),
                    IntegrationServiceMcpServer(
                        id = "mcp-server-2",
                        name = "MCP Server Two",
                        url = "https://server2.example.com",
                    ),
                )
                val mcpServersById = mcpServers.strictAssociateBy { it.id }

                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("mcpToolsByIds") { env ->
                                env.getArgument<List<String>>("ids")!!
                                    .map(mcpToolsById::get)
                            }
                            .dataFetcher("mcpServersByIds") { env ->
                                env.getArgument<List<String>>("ids")!!
                                    .map(mcpServersById::get)
                            }
                    }
            }
        ),
    ),
) {
    /**
     * Custom assertion to verify batch hydration works correctly with union types.
     *
     * Verifies:
     * 1. Only ONE batched call for all MCP tools
     * 2. Only ONE batched call for all MCP servers
     * 3. Both batched calls contain the correct IDs
     */
    override fun assert(result: ExecutionResult, incrementalResults: List<DelayedIncrementalPartialResult>?) {
        super.assert(result, incrementalResults)

        val snapshot = getTestSnapshot()

        // Verify batched call for MCP tools
        val mcpToolCalls = snapshot.calls.filter {
            it.service == "integration_service" && it.query.contains("mcpToolsByIds")
        }
        assertEquals(
            expected = 1,
            actual = mcpToolCalls.size,
            message = "Expected exactly ONE batched call for MCP tools, but found ${mcpToolCalls.size}"
        )

        val mcpToolCall = mcpToolCalls.first()
        assertTrue(
            mcpToolCall.query.contains("mcp-tool-1"),
            "MCP tools call should include mcp-tool-1 in the batch"
        )
        assertTrue(
            mcpToolCall.query.contains("mcp-tool-2"),
            "MCP tools call should include mcp-tool-2 in the batch"
        )

        // Verify batched call for MCP servers
        val mcpServerCalls = snapshot.calls.filter {
            it.service == "integration_service" && it.query.contains("mcpServersByIds")
        }
        assertEquals(
            expected = 1,
            actual = mcpServerCalls.size,
            message = "Expected exactly ONE batched call for MCP servers, but found ${mcpServerCalls.size}"
        )

        val mcpServerCall = mcpServerCalls.first()
        assertTrue(
            mcpServerCall.query.contains("mcp-server-1"),
            "MCP servers call should include mcp-server-1 in the batch"
        )
        assertTrue(
            mcpServerCall.query.contains("mcp-server-2"),
            "MCP servers call should include mcp-server-2 in the batch"
        )

        println("✅ VERIFIED: Batch hydration works with union types!")
        println("   - MCP Tools: 1 batched call for [mcp-tool-1, mcp-tool-2]")
        println("   - MCP Servers: 1 batched call for [mcp-server-1, mcp-server-2]")
        println("   - Regular tools: No hydration calls (as expected)")
    }
}

