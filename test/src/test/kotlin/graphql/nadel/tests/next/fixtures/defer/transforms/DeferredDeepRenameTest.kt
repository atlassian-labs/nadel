package graphql.nadel.tests.next.fixtures.defer.transforms

import graphql.nadel.NadelExecutionHints
import graphql.nadel.tests.next.NadelIntegrationTest

open class DeferredDeepRenameTest : NadelIntegrationTest(
    query = """
      query {
        ...@defer {
          details {
              name # Deep renamed from Issue.name
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
                  details: IssueDetail
                }
                type IssueDetail {
                  name: String @renamed(from: "issue.name")
                }

            """.trimIndent(),
            underlyingSchema = """
                directive @defer(if: Boolean, label: String) on FRAGMENT_SPREAD | INLINE_FRAGMENT

                type Issue {
                  name: String
                }
            
                type IssueDetail {
                  issue: Issue
                }
            
                type Query {
                  details: IssueDetail
                }

            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("details") { env ->
                                Any()
                            }
                    }
                    .type("IssueDetail") { type ->
                        type
                            .dataFetcher("issue") { env ->
                                Any()
                            }
                    }
                    .type("Issue") { type ->
                        type
                            .dataFetcher("name") { env ->
                                "Issue-1"
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
