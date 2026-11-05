package graphql.nadel.tests.next.fixtures.batching

import graphql.nadel.Service
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.hints.NadelBatchNamespacedRootFieldsHint
import graphql.nadel.hints.NadelBatchRootFieldsHint
import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.normalized.ExecutableNormalizedField
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NamespacedRootBatchingServiceIsolationTest {
    @Test
    fun `shared namespaces use each service's own child shards and opt ins`() = runTest {
        for (batchSearchRoots in listOf(false, true)) {
            for (batchSearchNamespaces in listOf(false, true)) {
                val capture = executeNamespacedQuery(
                    query = """
                        {
                            first: service { issue(cloudId: "one") search(cloudId: "two") }
                            second: service { issue(cloudId: "one") search(cloudId: "two") }
                            flatIssue: service_issue(cloudId: "one")
                            flatSearch: service_search(cloudId: "two")
                            flatSearchAgain: service_search(cloudId: "two")
                        }
                    """.trimIndent(),
                    services = listOf(namespacedBatchingService(), searchService()),
                    rootHint = object : NadelBatchRootFieldsHint {
                        override fun invoke(): Boolean = true
                        override fun invoke(service: Service): Boolean = service.name == "issues" || batchSearchRoots
                    },
                    namespaceHint = object : NadelBatchNamespacedRootFieldsHint {
                        override fun invoke(): Boolean = true
                        override fun invoke(service: Service): Boolean = service.name == "issues" || batchSearchNamespaces
                    },
                    hooks = object : CloudIdBatchingHooks() {
                        override fun getShardingTarget(
                            executionContext: NadelExecutionContext,
                            service: Service,
                            field: ExecutableNormalizedField,
                        ): Any? {
                            val expectedService = when (field.name) {
                                "issue", "service_issue" -> "issues"
                                "search", "service_search" -> "search"
                                else -> error("Unexpected sharding field: ${field.name}")
                            }
                            assertEquals(expectedService, service.name)
                            if (field.parent != null) {
                                assertEquals("service", field.parent.name)
                            }
                            return super.getShardingTarget(executionContext, service, field)
                        }
                    },
                )

                val namespaces = listOf(
                    "first: service { search(cloudId: \"two\") }",
                    "second: service { search(cloudId: \"two\") }",
                )
                val roots = listOf(
                    "rename__flatSearch__search: search(cloudId: \"two\")",
                    "rename__flatSearchAgain__search: search(cloudId: \"two\")",
                )
                val searchSelections = if (batchSearchNamespaces && batchSearchRoots) {
                    listOf((namespaces + roots).joinToString("\n"))
                } else {
                    (if (batchSearchNamespaces) listOf(namespaces.joinToString("\n")) else namespaces) +
                        (if (batchSearchRoots) listOf(roots.joinToString("\n")) else roots)
                }
                assertBatchingServiceQueries(
                    capture,
                    mapOf(
                        "issues" to listOf(
                            """
                                {
                                    first: service { issue(cloudId: "one") }
                                    second: service { issue(cloudId: "one") }
                                    rename__flatIssue__issue: issue(cloudId: "one")
                                }
                            """.trimIndent(),
                        ),
                        "search" to searchSelections.map { "{ $it }" },
                    ),
                )
                assertBatchingData(
                    capture,
                    mapOf(
                        "first" to mapOf("issue" to "one", "search" to "two"),
                        "second" to mapOf("issue" to "one", "search" to "two"),
                        "flatIssue" to "one",
                        "flatSearch" to "two",
                        "flatSearchAgain" to "two",
                    ),
                )
            }
        }
    }

    @Test
    fun `a shared namespace retains another service's data when one child fails`() = runTest {
        val capture = executeNamespacedQuery(
            query = """
                {
                    first: service {
                        broken: issue(cloudId: "one", fail: true)
                        healthy: search(cloudId: "two")
                    }
                    second: service { issue(cloudId: "one") search(cloudId: "two") }
                    flat: service_issue(cloudId: "one")
                }
            """.trimIndent(),
            services = listOf(namespacedBatchingService(), searchService()),
        )

        assertBatchingServiceQueries(
            capture,
            mapOf(
                "issues" to listOf(
                    """
                        {
                            first: service { broken: issue(cloudId: "one", fail: true) }
                            second: service { issue(cloudId: "one") }
                            rename__flat__issue: issue(cloudId: "one")
                        }
                    """.trimIndent(),
                ),
                "search" to listOf(
                    "{ first: service { healthy: search(cloudId: \"two\") } second: service { search(cloudId: \"two\") } }",
                ),
            ),
        )
        val result = requireNotNull(capture.result)
        assertEquals(
            mapOf(
                "first" to mapOf("broken" to null, "healthy" to "two"),
                "second" to mapOf("issue" to "one", "search" to "two"),
                "flat" to "one",
            ),
            requireNotNull(result.getData<Map<String, Any?>>()),
        )
        assertEquals(listOf("first", "broken"), result.errors.single().path)
    }

    private fun searchService(): NadelIntegrationTest.Service {
        return NadelIntegrationTest.Service(
            name = "search",
            overallSchema = """
                type Query {
                    service_search(cloudId: String): String @renamed(from: "search")
                }
                extend type ServiceQuery {
                    search(cloudId: String): String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                    service: ServiceQuery
                    search(cloudId: String): String
                }
                type ServiceQuery {
                    search(cloudId: String): String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("service") { emptyMap<String, Any?>() }
                            .dataFetcher("search") { env -> env.getArgument<String>("cloudId") ?: "unsharded" }
                    }
                    .type("ServiceQuery") { type ->
                        type.dataFetcher("search") { env -> env.getArgument<String>("cloudId") ?: "unsharded" }
                    }
            },
        )
    }
}
