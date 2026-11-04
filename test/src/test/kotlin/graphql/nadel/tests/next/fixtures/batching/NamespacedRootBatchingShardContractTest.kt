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
                    ambiguous: jira { issue(cloudId: "ambiguous") }
                    alsoAmbiguous: jira { issue(cloudId: "ambiguous") }
                    free: jira { issue }
                    flatFree: jira_issue
                    bound: jira { issue(cloudId: "one") }
                    flatBound: jira_issue(cloudId: "one")
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
            "{ ambiguous: jira { issue(cloudId: \"ambiguous\") } }",
            "{ alsoAmbiguous: jira { issue(cloudId: \"ambiguous\") } }",
            "{ free: jira { issue } rename__flatFree__issue: issue }",
            "{ bound: jira { issue(cloudId: \"one\") } rename__flatBound__issue: issue(cloudId: \"one\") }",
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
                    first: jira { issue }
                    second: jira { issue }
                    flatOne: jira_issue
                    flatTwo: jira_issue
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

        assertEquals(listOf("jira_issue", "jira_issue"), inspectedFields.toList())
        assertBatchingQueries(
            capture,
            "{ first: jira { issue } }",
            "{ second: jira { issue } }",
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
                    jira(cloudId: String): JiraQuery @namespaced
                    jira_issue(cloudId: String): String @renamed(from: "issue")
                }
                type JiraQuery { issue: String }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("jira") { env -> mapOf("cloudId" to env.getArgument<String>("cloudId")) }
                            .dataFetcher("issue") { env -> env.getArgument<String>("cloudId") }
                    }
                    .type("JiraQuery") { type ->
                        type.dataFetcher("issue") { env ->
                            requireNotNull(env.getSource<Map<String, String?>>())["cloudId"]
                        }
                    }
            },
        )
        val capture = executeNamespacedQuery(
            query = """
                {
                    first: jira(cloudId: "one") { issue }
                    second: jira(cloudId: "one") { issue }
                    other: jira(cloudId: "two") { issue }
                    flat: jira_issue(cloudId: "one")
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
                    first: jira(cloudId: "one") { issue }
                    second: jira(cloudId: "one") { issue }
                    rename__flat__issue: issue(cloudId: "one")
                }
            """.trimIndent(),
            "{ other: jira(cloudId: \"two\") { issue } }",
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
