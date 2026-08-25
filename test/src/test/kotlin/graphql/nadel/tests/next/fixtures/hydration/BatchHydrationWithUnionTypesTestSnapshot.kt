// @formatter:off
package graphql.nadel.tests.next.fixtures.hydration

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings

private suspend fun main() {
    graphql.nadel.tests.next.update<BatchHydrationWithUnionTypesTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused") class BatchHydrationWithUnionTypesTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "agent_studio",
                query = """
                | {
                |   scenario(id: "scenario-1") {
                |     id
                |     name
                |     tools {
                |       ... on AgentStudioMcpServer {
                |         __typename__batch_hydration__mcpServer: __typename
                |         id
                |         batch_hydration__mcpServer__id: id
                |       }
                |       ... on AgentStudioMcpTool {
                |         __typename__batch_hydration__mcpTool: __typename
                |         id
                |         batch_hydration__mcpTool__id: id
                |       }
                |       ... on AgentStudioTool {
                |         displayName
                |         id
                |         integrationKey
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "scenario": {
                |       "id": "scenario-1",
                |       "name": "My Test Scenario",
                |       "tools": [
                |         {
                |           "id": "tool-1",
                |           "displayName": "Regular Tool 1",
                |           "integrationKey": "regular-tool"
                |         },
                |         {
                |           "id": "mcp-tool-1",
                |           "batch_hydration__mcpTool__id": "mcp-tool-1",
                |           "__typename__batch_hydration__mcpTool": "AgentStudioMcpTool"
                |         },
                |         {
                |           "id": "tool-2",
                |           "displayName": "Regular Tool 2",
                |           "integrationKey": "another-regular-tool"
                |         },
                |         {
                |           "id": "mcp-tool-2",
                |           "batch_hydration__mcpTool__id": "mcp-tool-2",
                |           "__typename__batch_hydration__mcpTool": "AgentStudioMcpTool"
                |         },
                |         {
                |           "id": "mcp-server-1",
                |           "batch_hydration__mcpServer__id": "mcp-server-1",
                |           "__typename__batch_hydration__mcpServer": "AgentStudioMcpServer"
                |         },
                |         {
                |           "id": "mcp-server-2",
                |           "batch_hydration__mcpServer__id": "mcp-server-2",
                |           "__typename__batch_hydration__mcpServer": "AgentStudioMcpServer"
                |         }
                |       ]
                |     }
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "integration_service",
                query = """
                | {
                |   mcpServersByIds(ids: ["mcp-server-1", "mcp-server-2"]) {
                |     id
                |     batch_hydration__mcpServer__id: id
                |     name
                |     url
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "mcpServersByIds": [
                |       {
                |         "id": "mcp-server-1",
                |         "name": "MCP Server One",
                |         "url": "https://server1.example.com",
                |         "batch_hydration__mcpServer__id": "mcp-server-1"
                |       },
                |       {
                |         "id": "mcp-server-2",
                |         "name": "MCP Server Two",
                |         "url": "https://server2.example.com",
                |         "batch_hydration__mcpServer__id": "mcp-server-2"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "integration_service",
                query = """
                | {
                |   mcpToolsByIds(ids: ["mcp-tool-1", "mcp-tool-2"]) {
                |     description
                |     id
                |     batch_hydration__mcpTool__id: id
                |     name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "mcpToolsByIds": [
                |       {
                |         "id": "mcp-tool-1",
                |         "name": "MCP Tool Alpha",
                |         "description": "First MCP tool",
                |         "batch_hydration__mcpTool__id": "mcp-tool-1"
                |       },
                |       {
                |         "id": "mcp-tool-2",
                |         "name": "MCP Tool Beta",
                |         "description": "Second MCP tool",
                |         "batch_hydration__mcpTool__id": "mcp-tool-2"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
        )

    /**
     * ```json
     * {
     *   "data": {
     *     "scenario": {
     *       "id": "scenario-1",
     *       "name": "My Test Scenario",
     *       "tools": [
     *         {
     *           "id": "tool-1",
     *           "displayName": "Regular Tool 1",
     *           "integrationKey": "regular-tool"
     *         },
     *         {
     *           "id": "mcp-tool-1",
     *           "mcpTool": {
     *             "id": "mcp-tool-1",
     *             "name": "MCP Tool Alpha",
     *             "description": "First MCP tool"
     *           }
     *         },
     *         {
     *           "id": "tool-2",
     *           "displayName": "Regular Tool 2",
     *           "integrationKey": "another-regular-tool"
     *         },
     *         {
     *           "id": "mcp-tool-2",
     *           "mcpTool": {
     *             "id": "mcp-tool-2",
     *             "name": "MCP Tool Beta",
     *             "description": "Second MCP tool"
     *           }
     *         },
     *         {
     *           "id": "mcp-server-1",
     *           "mcpServer": {
     *             "id": "mcp-server-1",
     *             "name": "MCP Server One",
     *             "url": "https://server1.example.com"
     *           }
     *         },
     *         {
     *           "id": "mcp-server-2",
     *           "mcpServer": {
     *             "id": "mcp-server-2",
     *             "name": "MCP Server Two",
     *             "url": "https://server2.example.com"
     *           }
     *         }
     *       ]
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "scenario": {
            |       "id": "scenario-1",
            |       "name": "My Test Scenario",
            |       "tools": [
            |         {
            |           "id": "tool-1",
            |           "displayName": "Regular Tool 1",
            |           "integrationKey": "regular-tool"
            |         },
            |         {
            |           "id": "mcp-tool-1",
            |           "mcpTool": {
            |             "id": "mcp-tool-1",
            |             "name": "MCP Tool Alpha",
            |             "description": "First MCP tool"
            |           }
            |         },
            |         {
            |           "id": "tool-2",
            |           "displayName": "Regular Tool 2",
            |           "integrationKey": "another-regular-tool"
            |         },
            |         {
            |           "id": "mcp-tool-2",
            |           "mcpTool": {
            |             "id": "mcp-tool-2",
            |             "name": "MCP Tool Beta",
            |             "description": "Second MCP tool"
            |           }
            |         },
            |         {
            |           "id": "mcp-server-1",
            |           "mcpServer": {
            |             "id": "mcp-server-1",
            |             "name": "MCP Server One",
            |             "url": "https://server1.example.com"
            |           }
            |         },
            |         {
            |           "id": "mcp-server-2",
            |           "mcpServer": {
            |             "id": "mcp-server-2",
            |             "name": "MCP Server Two",
            |             "url": "https://server2.example.com"
            |           }
            |         }
            |       ]
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
