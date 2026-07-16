package graphql.nadel.tests.next.fixtures.batching

import graphql.nadel.NadelExecutionHints
import graphql.nadel.hints.NadelBatchRootFieldsHint
import graphql.nadel.tests.next.NadelIntegrationTest

/**
 * Root-field batching is opt-in per service. Here it is enabled for "batched" but not "unbatched".
 *
 * The snapshot should record:
 *  - a single call to "batched" containing batchedFoo and batchedBar, and
 *  - two separate calls to "unbatched" (one per root field), preserving today's behaviour.
 */
class BatchRootFieldsPerServiceTest : NadelIntegrationTest(
    query = """
        query {
          batchedFoo
          batchedBar
          unbatchedFoo
          unbatchedBar
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "batched",
            overallSchema = """
                type Query {
                  batchedFoo: String
                  batchedBar: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("batchedFoo") { "batchedFoo-value" }
                            .dataFetcher("batchedBar") { "batchedBar-value" }
                    }
            },
        ),
        Service(
            name = "unbatched",
            overallSchema = """
                type Query {
                  unbatchedFoo: String
                  unbatchedBar: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("unbatchedFoo") { "unbatchedFoo-value" }
                            .dataFetcher("unbatchedBar") { "unbatchedBar-value" }
                    }
            },
        ),
    ),
) {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints()
            .batchRootFields(object : NadelBatchRootFieldsHint {
                override fun invoke(): Boolean = true
                override fun invoke(service: graphql.nadel.Service): Boolean = service.name == "batched"
            })
    }
}
