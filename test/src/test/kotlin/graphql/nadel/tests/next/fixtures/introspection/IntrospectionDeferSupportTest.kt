package graphql.nadel.tests.next.fixtures.introspection

import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.ExperimentalApi
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.nadel.NadelExecutionHints
import graphql.nadel.ServiceExecution
import graphql.nadel.engine.blueprint.NadelDefaultIntrospectionRunner
import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.schema.GraphQLSchema
import kotlin.test.assertEquals

class IntrospectionDeferSupportEnabledTest : IntrospectionDeferSupportTestBase(deferSupport = true)

class IntrospectionDeferSupportDisabledTest : IntrospectionDeferSupportTestBase(deferSupport = false)

abstract class IntrospectionDeferSupportTestBase(
    private val deferSupport: Boolean,
) : NadelIntegrationTest(
    query = """
        query {
          ... @defer(label: "Body") {
            echo
            __typename
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "echo",
            overallSchema = """
                directive @defer(if: Boolean, label: String) on FRAGMENT_SPREAD | INLINE_FRAGMENT

                type Query {
                  echo: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("echo") {
                        "hello"
                    }
                }
            },
        ),
    ),
) {
    private val observedFlags = mutableListOf<Boolean>()

    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints()
            .deferSupport { deferSupport }
    }

    override fun makeIntrospectionRunner(schema: GraphQLSchema): ServiceExecution {
        return object : NadelDefaultIntrospectionRunner(schema) {
            override fun makeExecutionInput(input: ExecutionInput.Builder) {
                observedFlags.add(
                    input.build().graphQLContext.get<Boolean>(ExperimentalApi.ENABLE_INCREMENTAL_SUPPORT),
                )
            }
        }
    }

    override fun assert(result: ExecutionResult, incrementalResults: List<DelayedIncrementalPartialResult>?) {
        assertEquals(listOf(deferSupport), observedFlags)
    }
}
