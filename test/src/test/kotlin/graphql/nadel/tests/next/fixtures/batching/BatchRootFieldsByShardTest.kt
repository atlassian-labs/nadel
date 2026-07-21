package graphql.nadel.tests.next.fixtures.batching

import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionHints
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.normalized.ExecutableNormalizedField

/**
 * Root fields owned by the same service but routed to different shards must NOT be batched together.
 *
 * Here [getShardingTarget] returns the field's `cloudId` argument as the (opaque) shard key, so:
 *  - `a` + `b` (cloudId "site-1") share a shard and batch into a single call
 *  - `c` (cloudId "site-2") is a different shard and is sent separately
 *
 * The snapshot therefore records TWO calls to the "issues" service: one for site-1 (a + b) and one
 * for site-2 (c).
 */
class BatchRootFieldsByShardTest : NadelIntegrationTest(
    query = """
        query {
          a: issueById(cloudId: "site-1") { id }
          b: issueById(cloudId: "site-1") { id }
          c: issueById(cloudId: "site-2") { id }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "issues",
            overallSchema = """
                type Query {
                  issueById(cloudId: String!): Issue
                }
                type Issue {
                  id: ID
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("issueById") { env ->
                                mapOf("id" to env.getArgument<String>("cloudId"))
                            }
                    }
            },
        ),
    ),
) {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints()
            .batchRootFields { true }
    }

    override fun makeNadel(): Nadel.Builder {
        return super.makeNadel()
            .executionHooks(
                object : NadelExecutionHooks {
                    override fun getShardingTarget(
                        executionContext: NadelExecutionContext,
                        service: graphql.nadel.Service,
                        field: ExecutableNormalizedField,
                    ): Any? {
                        return field.resolvedArguments["cloudId"]
                    }
                },
            )
    }
}
