package graphql.nadel.tests.next.fixtures.batching

import graphql.nadel.Service
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.normalized.ExecutableNormalizedField
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NamespacedRootBatchingShardContractTest {
    @Test
    fun `fresh opaque targets keep ambiguous fields separate from concrete and null targets`() = runTest {
        val capture = executeNamespacedQuery(
            query = """
                {
                    ambiguous: service { issue(cloudId: "ambiguous") }
                    alsoAmbiguous: service { issue(cloudId: "ambiguous") }
                    free: service { issue }
                    flatFree: service_issue
                    bound: service { issue(cloudId: "one") }
                    flatBound: service_issue(cloudId: "one")
                }
            """.trimIndent(),
            hooks = object : CloudIdBatchingHooks() {
                override fun getShardingTarget(
                    executionContext: NadelExecutionContext,
                    service: Service,
                    field: ExecutableNormalizedField,
                ): Any? {
                    val target = super.getShardingTarget(executionContext, service, field)
                    return if (target == "ambiguous") Any() else target
                }
            },
        )

        assertBatchingQueries(
            capture,
            "{ ambiguous: service { issue(cloudId: \"ambiguous\") } }",
            "{ alsoAmbiguous: service { issue(cloudId: \"ambiguous\") } }",
            "{ free: service { issue } rename__flatFree__issue: issue }",
            "{ bound: service { issue(cloudId: \"one\") } rename__flatBound__issue: issue(cloudId: \"one\") }",
        )
        assertBatchingData(
            capture,
            mapOf(
                "ambiguous" to mapOf("issue" to "ambiguous"),
                "alsoAmbiguous" to mapOf("issue" to "ambiguous"),
                "free" to mapOf("issue" to "unsharded"),
                "flatFree" to "unsharded",
                "bound" to mapOf("issue" to "one"),
                "flatBound" to "one",
            ),
        )
    }

    @Test
    fun `disabled namespace batching never calls the shard hook for namespace children`() = runTest {
        val inspectedFields = ConcurrentLinkedQueue<String>()
        val capture = executeNamespacedQuery(
            query = """
                {
                    first: service { issue }
                    second: service { issue }
                    flatOne: service_issue
                    flatTwo: service_issue
                }
            """.trimIndent(),
            namespaceHint = null,
            hooks = object : CloudIdBatchingHooks() {
                override fun getShardingTarget(
                    executionContext: NadelExecutionContext,
                    service: Service,
                    field: ExecutableNormalizedField,
                ): Any? {
                    assertNull(field.parent)
                    inspectedFields.add(field.name)
                    return super.getShardingTarget(executionContext, service, field)
                }
            },
        )

        assertEquals(listOf("service_issue", "service_issue"), inspectedFields.toList())
        assertBatchingQueries(
            capture,
            "{ first: service { issue } }",
            "{ second: service { issue } }",
            "{ rename__flatOne__issue: issue rename__flatTwo__issue: issue }",
        )
        assertBatchingData(
            capture,
            mapOf(
                "first" to mapOf("issue" to "unsharded"),
                "second" to mapOf("issue" to "unsharded"),
                "flatOne" to "unsharded",
                "flatTwo" to "unsharded",
            ),
        )
    }

    @Test
    fun `child shard hooks can read routing arguments inherited from namespace wrappers`() = runTest {
        val service = NadelIntegrationTest.Service(
            name = "issues",
            overallSchema = """
                type Query {
                    service(cloudId: String): ServiceQuery @namespaced
                    service_issue(cloudId: String): String @renamed(from: "issue")
                }
                type ServiceQuery { issue: String }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("service") { env -> mapOf("cloudId" to env.getArgument<String>("cloudId")) }
                            .dataFetcher("issue") { env -> env.getArgument<String>("cloudId") }
                    }
                    .type("ServiceQuery") { type ->
                        type.dataFetcher("issue") { env ->
                            requireNotNull(env.getSource<Map<String, String?>>())["cloudId"]
                        }
                    }
            },
        )
        val capture = executeNamespacedQuery(
            query = """
                {
                    first: service(cloudId: "one") { issue }
                    second: service(cloudId: "one") { issue }
                    other: service(cloudId: "two") { issue }
                    flat: service_issue(cloudId: "one")
                }
            """.trimIndent(),
            services = listOf(service),
            hooks = object : CloudIdBatchingHooks() {
                override fun getShardingTarget(
                    executionContext: NadelExecutionContext,
                    service: Service,
                    field: ExecutableNormalizedField,
                ): Any? {
                    return field.parent?.resolvedArguments?.get("cloudId")
                        ?: super.getShardingTarget(executionContext, service, field)
                }
            },
        )

        assertBatchingQueries(
            capture,
            """
                {
                    first: service(cloudId: "one") { issue }
                    second: service(cloudId: "one") { issue }
                    rename__flat__issue: issue(cloudId: "one")
                }
            """.trimIndent(),
            "{ other: service(cloudId: \"two\") { issue } }",
        )
        assertBatchingData(
            capture,
            mapOf(
                "first" to mapOf("issue" to "one"),
                "second" to mapOf("issue" to "one"),
                "other" to mapOf("issue" to "two"),
                "flat" to "one",
            ),
        )
    }
}
