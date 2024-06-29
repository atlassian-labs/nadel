package graphql.nadel.tests.next.fixtures.defer.transforms

import graphql.nadel.NadelExecutionHints
import graphql.nadel.tests.next.NadelIntegrationTest

open class MultipleRenamedFieldsAreDeferredTogether : NadelIntegrationTest(
    query = """
      query {
        defer {
          hello
          ...@defer {
              overallString
              overallString2
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
                  overallString: String @renamed(from: "underlyingString")
                  overallString2: String @renamed(from: "underlyingString2")
                }
               
            """.trimIndent(),
            underlyingSchema = """
                directive @defer(if: Boolean, label: String) on FRAGMENT_SPREAD | INLINE_FRAGMENT

                type Query {
                  defer: DeferApi
                }
                type DeferApi {
                  hello: String
                  underlyingString: String
                  underlyingString2: String
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
                            .dataFetcher("hello") { env ->
                                "hello there"
                            }
                            .dataFetcher("underlyingString") { env ->
                                "deferred string 1"
                            }
                            .dataFetcher("underlyingString2") { env ->
                                "deferred string 2"
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
