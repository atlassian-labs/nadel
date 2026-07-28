package graphql.nadel.tests.next.fixtures.defer

import graphql.nadel.NadelExecutionHints
import graphql.nadel.tests.next.NadelIntegrationTest

open class NamedFragmentDeferAtRootTest : NadelIntegrationTest(
    query = """
      query RootLevelFragmentDeferQuery {
        greeting
        ...RecommendationsDeferred @defer(label: "recommendations")
      }

      fragment RecommendationsDeferred on Query {
        recommendations {
          items
        }
      }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "defer",
            overallSchema = """
                directive @defer(if: Boolean, label: String) on FRAGMENT_SPREAD | INLINE_FRAGMENT

                type Query {
                  greeting: String
                  recommendations: Recommendations
                }
                type Recommendations {
                  items: [String!]!
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("greeting") { env ->
                                "helloString"
                            }
                            .dataFetcher("recommendations") { env ->
                                Any()
                            }
                    }
                    .type("Recommendations") { type ->
                        type
                            .dataFetcher("items") { env ->
                                listOf("first", "second")
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
}
