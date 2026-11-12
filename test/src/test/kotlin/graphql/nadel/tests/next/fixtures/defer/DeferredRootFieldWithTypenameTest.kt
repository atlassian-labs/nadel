package graphql.nadel.tests.next.fixtures.defer

import graphql.ExecutionResult
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.nadel.NadelExecutionHints
import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.nadel.tests.next.combineExecutionResults
import kotlin.test.assertEquals

class DeferredRootFieldWithTypenameTest : NadelIntegrationTest(
    query = """
        query Repro {
          ... @defer(label: "Body") {
            me {
              user { id }
            }
            __typename
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "users",
            overallSchema = """
                directive @defer(if: Boolean, label: String) on FRAGMENT_SPREAD | INLINE_FRAGMENT

                type Query {
                  me: Me
                }
                type Me {
                  user: User
                }
                type User {
                  id: ID!
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type.dataFetcher("me") {
                            mapOf("user" to mapOf("id" to "user-1"))
                        }
                    }
            },
        ),
    ),
) {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints()
            .deferSupport { true }
    }

    override fun assert(result: ExecutionResult, incrementalResults: List<DelayedIncrementalPartialResult>?) {
        // Given
        val expected = mapOf(
            "data" to mapOf(
                "me" to mapOf("user" to mapOf("id" to "user-1")),
                "__typename" to "Query",
            ),
        )

        // When
        val combinedResult = combineExecutionResults(
            result = result.toSpecification(),
            incrementalResults = incrementalResults.orEmpty()
                .map(DelayedIncrementalPartialResult::toSpecification),
        )

        // Then
        assertEquals(expected, combinedResult)
    }
}
