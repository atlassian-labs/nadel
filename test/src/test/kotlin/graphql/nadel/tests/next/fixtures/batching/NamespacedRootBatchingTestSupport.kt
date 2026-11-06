package graphql.nadel.tests.next.fixtures.batching

import graphql.GraphqlErrorBuilder
import graphql.execution.DataFetcherResult
import graphql.language.AstPrinter
import graphql.language.AstSorter
import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionHints
import graphql.nadel.Service
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.hints.NadelBatchNamespacedRootFieldsHint
import graphql.nadel.hints.NadelBatchRootFieldsHint
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.nadel.tests.next.TestExecutionCapture
import graphql.normalized.ExecutableNormalizedField
import graphql.parser.Parser
import graphql.schema.DataFetchingEnvironment
import kotlin.test.assertEquals

internal suspend fun executeNamespacedQuery(
    query: String,
    rootHint: NadelBatchRootFieldsHint = NadelBatchRootFieldsHint { true },
    namespaceHint: NadelBatchNamespacedRootFieldsHint? = NadelBatchNamespacedRootFieldsHint { true },
    services: List<NadelIntegrationTest.Service> = listOf(namespacedBatchingService()),
    hooks: NadelExecutionHooks = CloudIdBatchingHooks(),
): TestExecutionCapture {
    return object : NadelIntegrationTest(query = query, services = services) {
        override fun makeExecutionHints(): NadelExecutionHints.Builder {
            return super.makeExecutionHints()
                .batchRootFields(rootHint)
                .also { hints ->
                    namespaceHint?.let { hints.batchNamespacedRootFields(it) }
                }
        }

        override fun makeNadel(): Nadel.Builder {
            return super.makeNadel().executionHooks(hooks)
        }
    }.capture()
}

internal open class CloudIdBatchingHooks : NadelExecutionHooks {
    override fun getShardingTarget(
        executionContext: NadelExecutionContext,
        service: Service,
        field: ExecutableNormalizedField,
    ): Any? {
        return field.resolvedArguments["cloudId"]
    }
}

internal fun namespacedBatchingService(): NadelIntegrationTest.Service {
    return NadelIntegrationTest.Service(
        name = "issues",
        overallSchema = """
            type Query {
                service: ServiceQuery @namespaced
                service_issue(cloudId: String, fail: Boolean): String @renamed(from: "issue")
                partitioned(ids: [ID!]!): [String] @partition(pathToPartitionArg: ["ids"])
            }
            type ServiceQuery {
                issue(cloudId: String, fail: Boolean): String
                partitioned(ids: [ID!]!): [String] @partition(pathToPartitionArg: ["ids"])
            }
            type Mutation {
                service: ServiceMutation @namespaced
                service_update(value: String!): String @renamed(from: "update")
            }
            type ServiceMutation {
                update(value: String!): String
            }
        """.trimIndent(),
        runtimeWiring = { wiring ->
            wiring
                .type("Query") { type ->
                    type
                        .dataFetcher("service") { emptyMap<String, Any?>() }
                        .dataFetcher("issue", ::fetchIssue)
                        .dataFetcher("partitioned") { env -> env.getArgument<List<String>>("ids") }
                }
                .type("ServiceQuery") { type ->
                    type
                        .dataFetcher("issue", ::fetchIssue)
                        .dataFetcher("partitioned") { env -> env.getArgument<List<String>>("ids") }
                }
                .type("Mutation") { type ->
                    type
                        .dataFetcher("service") { emptyMap<String, Any?>() }
                        .dataFetcher("update") { env -> env.getArgument<String>("value") }
                }
                .type("ServiceMutation") { type ->
                    type.dataFetcher("update") { env -> env.getArgument<String>("value") }
                }
        },
    )
}

private fun fetchIssue(env: DataFetchingEnvironment): Any? {
    return if (env.getArgument<Boolean>("fail") == true) {
        DataFetcherResult.newResult<String>()
            .data(null)
            .error(
                GraphqlErrorBuilder.newError()
                    .message("Issue lookup failed")
                    .path(env.executionStepInfo.path.toList())
                    .build(),
            )
            .build()
    } else {
        env.getArgument<String>("cloudId") ?: "unsharded"
    }
}

internal fun assertBatchingQueries(capture: TestExecutionCapture, vararg queries: String) {
    assertBatchingServiceQueries(capture, mapOf("issues" to queries.toList()))
}

internal fun assertBatchingServiceQueries(
    capture: TestExecutionCapture,
    queries: Map<String, List<String>>,
) {
    fun canonical(query: String): String {
        return AstPrinter.printAst(AstSorter().sort(Parser.parse(query)))
    }

    assertEquals(
        queries.mapValues { (_, documents) -> documents.map(::canonical).sorted() },
        capture.calls.groupBy { it.service }
            .mapValues { (_, calls) -> calls.map { canonical(it.query) }.sorted() },
    )
}

internal fun assertBatchingData(capture: TestExecutionCapture, expected: Map<String, Any?>) {
    val result = requireNotNull(capture.result)
    assertEquals(emptyList(), result.errors)
    assertEquals(expected, result.getData())
}
