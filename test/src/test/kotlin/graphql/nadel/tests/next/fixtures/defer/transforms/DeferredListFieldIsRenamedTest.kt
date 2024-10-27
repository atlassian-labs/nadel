package graphql.nadel.tests.next.fixtures.defer.transforms

import graphql.nadel.NadelExecutionHints
import graphql.nadel.tests.next.NadelIntegrationTest

open class DeferredListFieldIsRenamedTest : NadelIntegrationTest(
    query = """
      query {
        ...@defer {
          issues {
            key
            assigneeId
            awesomeIssueName
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
                  issues: [Issue!]
                }
                
                type Issue {
                  key: String!
                  assigneeId: ID!
                  awesomeIssueName: String @renamed(from: "title")
                  related: [Issue!]
                  parent: Issue
                }
            """.trimIndent(),
            underlyingSchema = """
                directive @defer(if: Boolean, label: String) on FRAGMENT_SPREAD | INLINE_FRAGMENT

                type Query {
                  issues: [Issue!]
                }
                
                type Issue {
                  key: String!
                  assigneeId: ID!
                  title: String
                  related: [Issue!]
                  parent: Issue
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Issue(
                    val key: String,
                    val assigneeId: String,
                    val title: String? = null,
                    val parentKey: String? = null,
                    val relatedKeys: List<String> = emptyList(),
                )
                val issues = listOf(
                    Issue(
                        key = "GQLGW-1",
                        assigneeId = "ari:cloud:identity::user/1",
                        title = "Issue 1",
                    ),
                    Issue(
                        key = "GQLGW-2",
                        assigneeId = "ari:cloud:identity::user/2",
                        parentKey = "GQLGW-1",
                        relatedKeys = listOf("GQLGW-1"),
                        title = "Issue 2",
                    ),
                    Issue(
                        key = "GQLGW-3",
                        assigneeId = "ari:cloud:identity::user/1",
                        parentKey = "GQLGW-1",
                        relatedKeys = listOf("GQLGW-1", "GQLGW-2"),
                        title = "Issue 3",
                    ),
                    Issue(
                        key = "GQLGW-4",
                        assigneeId = "ari:cloud:identity::user/3",
                        parentKey = "GQLGW-1",
                        relatedKeys = listOf("GQLGW-1", "GQLGW-2", "GQLGW-3"),
                        // no title here to test rename on null result
                    ),
                )
                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("issues") { env ->
                                issues
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