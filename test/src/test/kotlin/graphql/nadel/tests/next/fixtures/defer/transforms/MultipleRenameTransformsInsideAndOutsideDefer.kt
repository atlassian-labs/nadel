package graphql.nadel.tests.next.fixtures.defer.transforms

import graphql.nadel.NadelExecutionHints
import graphql.nadel.tests.next.NadelIntegrationTest

open class MultipleRenameTransformsInsideAndOutsideDefer : NadelIntegrationTest(
    query = """
      query {
        defer {
          fastRenamedString
          ...@defer {
              slowRenamedString
          }
        }
      }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "defer",
            overallSchema = """
                directive @defer(if: Boolean, label: String) on FRAGMENT_SPREAD | INLINE_FRAGMENT

                type Query {
                  defer: DeferApi 
                }
                type DeferApi {
                  hello: String
                  slowRenamedString: String @renamed(from: "slowString")
                  fastRenamedString: String @renamed(from: "fastString")
                }

            """.trimIndent(),
            underlyingSchema = """
                directive @defer(if: Boolean, label: String) on FRAGMENT_SPREAD | INLINE_FRAGMENT

                type Query {
                  defer: DeferApi
                }
                type DeferApi {
                  hello: String
                  slowString: String
                  fastString: String
                }

            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("defer") { env ->
                                Any()
                            }
                    }
                    .type("DeferApi") { type ->
                        type
                            .dataFetcher("slowString") { env ->
                                "this is the slow string (deferred)"
                            }
                            .dataFetcher("fastString") { env ->
                                "this is the fast string (not deferred)"
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