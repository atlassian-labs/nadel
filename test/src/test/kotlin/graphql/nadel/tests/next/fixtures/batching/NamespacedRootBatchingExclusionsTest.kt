package graphql.nadel.tests.next.fixtures.batching

import graphql.language.Field
import graphql.language.OperationDefinition
import graphql.nadel.Service
import graphql.nadel.engine.transform.partition.NadelPartitionTransformHook
import graphql.nadel.hints.NadelBatchNamespacedRootFieldsHint
import graphql.nadel.hooks.ServiceOrError
import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.nadel.tests.next.fixtures.partition.hooks.RoutingBasedPartitionTransformHook
import graphql.normalized.ExecutableNormalizedField
import graphql.parser.Parser
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NamespacedRootBatchingExclusionsTest {
    @Test
    fun `partitioned namespace stays separate while other wrappers and roots batch`() = runTest {
        val capture = executeNamespacedQuery(
            query = """
                {
                    split: jira { partitioned(ids: ["one:A", "two:B"]) }
                    first: jira { issue }
                    second: jira { issue }
                    flat: jira_issue
                }
            """.trimIndent(),
            hooks = partitionHooks(),
        )

        assertBatchingQueries(
            capture,
            "{ split: jira { partitioned(ids: [\"one:A\"]) } }",
            "{ split: jira { partitioned(ids: [\"two:B\"]) } }",
            "{ first: jira { issue } second: jira { issue } rename__flat__issue: issue }",
        )
        assertBatchingData(
            capture,
            mapOf(
                "split" to mapOf("partitioned" to listOf("one:A", "two:B")),
                "first" to mapOf("issue" to "unsharded"),
                "second" to mapOf("issue" to "unsharded"),
                "flat" to "unsharded",
            ),
        )
    }

    @Test
    fun `ordinary batch containing a partition keeps its existing membership`() = runTest {
        val capture = executeNamespacedQuery(
            query = """
                {
                    partitioned(ids: ["one:A", "two:B"])
                    flat: jira_issue
                    first: jira { issue }
                    second: jira { issue }
                }
            """.trimIndent(),
            hooks = partitionHooks(),
        )

        assertBatchingQueries(
            capture,
            "{ partitioned(ids: [\"one:A\"]) rename__flat__issue: issue }",
            "{ partitioned(ids: [\"two:B\"]) }",
            "{ first: jira { issue } second: jira { issue } }",
        )
        assertBatchingData(
            capture,
            mapOf(
                "partitioned" to listOf("one:A", "two:B"),
                "flat" to "unsharded",
                "first" to mapOf("issue" to "unsharded"),
                "second" to mapOf("issue" to "unsharded"),
            ),
        )
    }

    @Test
    fun `namespace flag does not change mutation requests or ordinary mutation batching`() = runTest {
        for (enabled in listOf(false, true)) {
            val capture = executeNamespacedQuery(
                query = """
                    mutation {
                        first: jira { update(value: "one") }
                        second: jira { update(value: "two") }
                        flatOne: jira_update(value: "three")
                        flatTwo: jira_update(value: "four")
                    }
                """.trimIndent(),
                namespaceHint = NadelBatchNamespacedRootFieldsHint { enabled },
            )

            assertBatchingQueries(
                capture,
                "mutation { first: jira { update(value: \"one\") } }",
                "mutation { second: jira { update(value: \"two\") } }",
                """
                    mutation {
                        rename__flatOne__update: update(value: "three")
                        rename__flatTwo__update: update(value: "four")
                    }
                """.trimIndent(),
            )
            assertBatchingData(
                capture,
                mapOf(
                    "first" to mapOf("update" to "one"),
                    "second" to mapOf("update" to "two"),
                    "flatOne" to "three",
                    "flatTwo" to "four",
                ),
            )
        }
    }

    @Test
    fun `introspection stays local while service selections batch`() = runTest {
        val capture = executeNamespacedQuery(
            query = """
                {
                    rootType: __typename
                    first: jira { namespaceType: __typename issue }
                    second: jira { issue }
                    typeOnly: jira { name: __typename }
                    flat: jira_issue
                }
            """.trimIndent(),
        )

        assertBatchingQueries(
            capture,
            "{ first: jira { issue } second: jira { issue } rename__flat__issue: issue }",
        )
        assertBatchingData(
            capture,
            mapOf(
                "rootType" to "Query",
                "first" to mapOf("namespaceType" to "JiraQuery", "issue" to "unsharded"),
                "second" to mapOf("issue" to "unsharded"),
                "typeOnly" to mapOf("name" to "JiraQuery"),
                "flat" to "unsharded",
            ),
        )
    }

    @Test
    fun `dynamic roots and namespaces containing dynamic children stay separate`() = runTest {
        val service = NadelIntegrationTest.Service(
            name = "issues",
            overallSchema = """
                directive @dynamicServiceResolution on FIELD_DEFINITION
                type Query {
                    jira: JiraQuery @namespaced
                    node(id: ID!): Node @dynamicServiceResolution
                    plain: String
                }
                type JiraQuery {
                    node(id: ID!): Node @dynamicServiceResolution
                    plain: String
                }
                interface Node { id: ID! }
                type Item implements Node { id: ID! }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                for (typeName in listOf("Query", "JiraQuery")) {
                    wiring.type(typeName) { type ->
                        type
                            .dataFetcher("plain") { "ok" }
                            .dataFetcher("node") { env -> mapOf("id" to env.getArgument<String>("id")) }
                    }
                }
                wiring.type("Query") { type -> type.dataFetcher("jira") { emptyMap<String, Any?>() } }
                wiring.type("Node") { type -> type.typeResolver { env -> env.schema.getObjectType("Item") } }
            },
        )
        val capture = executeNamespacedQuery(
            query = """
                {
                    dynamic: jira { node(id: "child") { id } }
                    first: jira { plain }
                    second: jira { plain }
                    top: node(id: "root") { id }
                    plain
                }
            """.trimIndent(),
            services = listOf(service),
            hooks = object : CloudIdBatchingHooks() {
                override fun resolveServiceForField(
                    services: List<Service>,
                    executableNormalizedField: ExecutableNormalizedField,
                ): ServiceOrError {
                    assertEquals("top", executableNormalizedField.resultKey)
                    return ServiceOrError(services.single(), null)
                }
            },
        )

        val rootsByCall = capture.calls.map { call ->
            Parser.parse(call.query).definitions.filterIsInstance<OperationDefinition>().single()
                .selectionSet.selections.filterIsInstance<Field>()
                .map { it.alias ?: it.name }.toSet()
        }
        assertEquals(
            setOf(setOf("dynamic"), setOf("top"), setOf("first", "second", "plain")),
            rootsByCall.toSet(),
        )
        assertEquals(3, capture.calls.size)
        assertBatchingData(
            capture,
            mapOf(
                "dynamic" to mapOf("node" to mapOf("id" to "child")),
                "first" to mapOf("plain" to "ok"),
                "second" to mapOf("plain" to "ok"),
                "top" to mapOf("id" to "root"),
                "plain" to "ok",
            ),
        )
    }

    private fun partitionHooks(): CloudIdBatchingHooks {
        return object : CloudIdBatchingHooks() {
            override fun partitionTransformerHook(): NadelPartitionTransformHook {
                return RoutingBasedPartitionTransformHook()
            }
        }
    }
}
