package graphql.nadel.tests.next.fixtures.defer

import graphql.nadel.NadelExecutionHints
import graphql.nadel.tests.next.NadelIntegrationTest
import kotlin.test.Ignore

@Ignore("Ignored for now as test is failing. This will be implemented later. It may be a problem with ExecutableNormalisedFields in graphql-java")
open class NestedDefersTest : NadelIntegrationTest(
    query = """
      query {
        defer {
          fastField
          ... @defer(label: "outer defer") {
            slowField1
            ... @defer(label: "inner defer") {
                slowField2
            }
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
                  fastField: String
                  slowField1: String
                  slowField2: String
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
                                "helloString"
                            }
                            .dataFetcher("slow1") { env ->
                                Thread.sleep(1_000)  // wait for 1 second
                                "slowString 1"
                            }
                            .dataFetcher("slow2") { env ->
                                "slowString 2"
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
