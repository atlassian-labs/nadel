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
                    split: service { partitioned(ids: ["one:A", "two:B"]) }
                    first: service { issue }
                    second: service { issue }
                    flat: service_issue
                }
            """.trimIndent(),
            hooks = partitionHooks(),
        )

        assertBatchingQueries(
            capture,
            "{ split: service { partitioned(ids: [\"one:A\"]) } }",
            "{ split: service { partitioned(ids: [\"two:B\"]) } }",
            "{ first: service { issue } second: service { issue } rename__flat__issue: issue }",
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
    fun `partitioned query roots stay separate regardless of namespace batching`() = runTest {
        for (namespaceBatchingEnabled in listOf(false, true)) {
            val capture = executeNamespacedQuery(
                query = """
                    {
                        partitioned(ids: ["one:A", "two:B"])
                        flat: service_issue
                        first: service { issue }
                        second: service { issue }
                        reversed: partitioned(ids: ["two:B", "one:A"])
                    }
                """.trimIndent(),
                namespaceHint = NadelBatchNamespacedRootFieldsHint { namespaceBatchingEnabled },
                hooks = partitionHooks(),
            )

            val nonPartitionedQueries = if (namespaceBatchingEnabled) {
                arrayOf("{ first: service { issue } second: service { issue } rename__flat__issue: issue }")
            } else {
                arrayOf(
                    "{ first: service { issue } }",
                    "{ second: service { issue } }",
                    "{ rename__flat__issue: issue }",
                )
            }
            assertBatchingQueries(
                capture,
                "{ partitioned(ids: [\"one:A\"]) }",
                "{ partitioned(ids: [\"two:B\"]) }",
                "{ reversed: partitioned(ids: [\"two:B\"]) }",
                "{ reversed: partitioned(ids: [\"one:A\"]) }",
                *nonPartitionedQueries,
            )
            assertBatchingData(
                capture,
                mapOf(
                    "partitioned" to listOf("one:A", "two:B"),
                    "reversed" to listOf("two:B", "one:A"),
                    "flat" to "unsharded",
                    "first" to mapOf("issue" to "unsharded"),
                    "second" to mapOf("issue" to "unsharded"),
                ),
            )
        }
    }

    @Test
    fun `partitioned mutation keeps its existing batch regardless of namespace batching`() = runTest {
        val service = NadelIntegrationTest.Service(
            name = "issues",
            overallSchema = """
                type Query { unused: String }
                type Mutation {
                    partitioned(ids: [ID!]!): [String] @partition(pathToPartitionArg: ["ids"])
                    update(value: String!): String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Mutation") { type ->
                    type
                        .dataFetcher("partitioned") { env -> env.getArgument<List<String>>("ids") }
                        .dataFetcher("update") { env -> env.getArgument<String>("value") }
                }
            },
        )
        for (namespaceBatchingEnabled in listOf(false, true)) {
            val capture = executeNamespacedQuery(
                query = """
                    mutation {
                        partitioned(ids: ["one:A", "two:B"])
                        update(value: "three")
                    }
                """.trimIndent(),
                namespaceHint = NadelBatchNamespacedRootFieldsHint { namespaceBatchingEnabled },
                services = listOf(service),
                hooks = partitionHooks(),
            )

            assertBatchingQueries(
                capture,
                "mutation { partitioned(ids: [\"one:A\"]) update(value: \"three\") }",
                "mutation { partitioned(ids: [\"two:B\"]) }",
            )
            assertBatchingData(
                capture,
                mapOf("partitioned" to listOf("one:A", "two:B"), "update" to "three"),
            )
        }
    }

    @Test
    fun `namespace flag does not change mutation requests or ordinary mutation batching`() = runTest {
        for (enabled in listOf(false, true)) {
            val capture = executeNamespacedQuery(
                query = """
                    mutation {
                        first: service { update(value: "one") }
                        second: service { update(value: "two") }
                        flatOne: service_update(value: "three")
                        flatTwo: service_update(value: "four")
                    }
                """.trimIndent(),
                namespaceHint = NadelBatchNamespacedRootFieldsHint { enabled },
            )

            assertBatchingQueries(
                capture,
                "mutation { first: service { update(value: \"one\") } }",
                "mutation { second: service { update(value: \"two\") } }",
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
                    first: service { namespaceType: __typename issue }
                    second: service { issue }
                    typeOnly: service { name: __typename }
                    flat: service_issue
                }
            """.trimIndent(),
        )

        assertBatchingQueries(
            capture,
            "{ first: service { issue } second: service { issue } rename__flat__issue: issue }",
        )
        assertBatchingData(
            capture,
            mapOf(
                "rootType" to "Query",
                "first" to mapOf("namespaceType" to "ServiceQuery", "issue" to "unsharded"),
                "second" to mapOf("issue" to "unsharded"),
                "typeOnly" to mapOf("name" to "ServiceQuery"),
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
                    service: ServiceQuery @namespaced
                    node(id: ID!): Node @dynamicServiceResolution
                    plain: String
                }
                type ServiceQuery {
                    node(id: ID!): Node @dynamicServiceResolution
                    plain: String
                }
                interface Node { id: ID! }
                type Item implements Node { id: ID! }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                for (typeName in listOf("Query", "ServiceQuery")) {
                    wiring.type(typeName) { type ->
                        type
                            .dataFetcher("plain") { "ok" }
                            .dataFetcher("node") { env -> mapOf("id" to env.getArgument<String>("id")) }
                    }
                }
                wiring.type("Query") { type -> type.dataFetcher("service") { emptyMap<String, Any?>() } }
                wiring.type("Node") { type -> type.typeResolver { env -> env.schema.getObjectType("Item") } }
            },
        )
        val capture = executeNamespacedQuery(
            query = """
                {
                    dynamic: service { node(id: "child") { id } }
                    first: service { plain }
                    second: service { plain }
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
