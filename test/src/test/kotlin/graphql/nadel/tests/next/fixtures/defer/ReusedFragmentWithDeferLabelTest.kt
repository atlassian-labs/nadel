package graphql.nadel.tests.next.fixtures.defer

import graphql.nadel.NadelExecutionHints
import graphql.nadel.tests.next.NadelIntegrationTest

class ReusedFragmentWithDeferLabelTest : NadelIntegrationTest(
    operationName = "DeferredTimeTrackingRepro",
    query = """
        query DeferredTimeTrackingRepro(
          ${'$'}cloudId: ID!
          ${'$'}issueKey: String!
        ) {
          jira {
            issueByKey(cloudId: ${'$'}cloudId, key: ${'$'}issueKey) {
              id
              ...Sidebar
              ...Details
            }
          }
        }

        fragment Sidebar on JiraIssue {
          ...TimeTrackingField
        }

        fragment Details on JiraIssue {
          ...TimeTrackingField
        }

        fragment TimeTrackingField on JiraIssue {
          timeTrackingField {
            id
            ...TimeTrackingData @defer(label: "timeTracking")
          }
        }

        fragment TimeTrackingData on JiraTimeTrackingField {
          originalEstimateOnChildIssues {
            timeInSeconds
          }
          remainingEstimateOnChildIssues {
            timeInSeconds
          }
        }
    """.trimIndent(),
    variables = mapOf(
        "cloudId" to "cloud-1",
        "issueKey" to "TEST-1",
    ),
    services = listOf(
        Service(
            name = "jira",
            overallSchema = """
                directive @defer(if: Boolean, label: String) on FRAGMENT_SPREAD | INLINE_FRAGMENT

                type Query {
                  jira: Jira
                }
                type Jira {
                  issueByKey(cloudId: ID!, key: String!): JiraIssue
                }
                type JiraIssue {
                  id: ID
                  timeTrackingField: JiraTimeTrackingField
                }
                type JiraTimeTrackingField {
                  id: ID
                  originalEstimateOnChildIssues: JiraTimeEstimate
                  remainingEstimateOnChildIssues: JiraTimeEstimate
                }
                type JiraTimeEstimate {
                  timeInSeconds: Int
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type.dataFetcher("jira") { _ ->
                            Any()
                        }
                    }
                    .type("Jira") { type ->
                        type.dataFetcher("issueByKey") { _ ->
                            mapOf(
                                "id" to "issue-1",
                                "timeTrackingField" to mapOf(
                                    "id" to "time-tracking-1",
                                    "originalEstimateOnChildIssues" to mapOf("timeInSeconds" to 3600),
                                    "remainingEstimateOnChildIssues" to mapOf("timeInSeconds" to 1800),
                                ),
                            )
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
