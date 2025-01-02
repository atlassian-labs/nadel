package graphql.nadel.tests.legacy.`chained transforms`.`ari use case`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `ari argument in renamed object input in hydration` : NadelLegacyIntegrationTest(
    query = """
        query {
          issue(id: "ari:cloud:jira-software::issue/123") {
            related {
              projectId
              key
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "MyService",
            overallSchema = """
                type Query {
                  issue(id: ID!): Issue
                  issues(input: [IssueInput]): [Issue]
                }
                type Issue {
                  id: ID! @ARI(type: "issue", owner: "jira-software", interpreted: true)
                  projectId: ID! @ARI(type: "project", owner: "jira-software", interpreted: true)
                  key: String!
                  related: [Issue] @hydrated(
                    service: "MyService"
                    field: "issues"
                    arguments: [{name: "input" value: "${'$'}source.relatedIds"}]
                    inputIdentifiedBy: [
                      {sourceId: "relatedIds.projectId" resultId: "projectId"}
                      {sourceId: "relatedIds.issueId" resultId: "id"}
                    ]
                  )
                }
                input IssueInput @renamed(from: "UnderlyingIssueInput") {
                  projectId: ID! @ARI(type: "project", owner: "jira-software", interpreted: true)
                  issueId: ID! @ARI(type: "issue", owner: "jira-software", interpreted: true)
                }
                type Sprint {
                  id: ID!
                }
                directive @ARI(
                  type: String!
                  owner: String!
                  interpreted: Boolean! = false
                ) on ARGUMENT_DEFINITION | FIELD_DEFINITION | INPUT_FIELD_DEFINITION
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  issue(id: ID!): Issue
                  issues(input: [UnderlyingIssueInput]): [Issue]
                }
                type Issue {
                  id: ID!
                  projectId: ID!
                  key: String!
                  relatedIds: [RelatedIssue]
                }
                type RelatedIssue {
                  projectId: ID!
                  issueId: ID!
                }
                input UnderlyingIssueInput {
                  projectId: ID!
                  issueId: ID!
                }
                type Sprint {
                  id: ID!
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type
                        .dataFetcher("issue") { env ->
                            if (env.getArgument<Any?>("id") == "ari:cloud:jira-software::issue/123") {
                                MyService_Issue(
                                    relatedIds = listOf(
                                        MyService_RelatedIssue(
                                            projectId = "ari:cloud:jira-software::project/100",
                                            issueId = "ari:cloud:jira-software::issue/1",
                                        ),
                                        MyService_RelatedIssue(
                                            projectId = "ari:cloud:jira-software::project/100",
                                            issueId = "ari:cloud:jira-software::issue/2",
                                        ),
                                        MyService_RelatedIssue(
                                            projectId = "ari:cloud:jira-software::project/101",
                                            issueId = "ari:cloud:jira-software::issue/3",
                                        ),
                                    ),
                                )
                            } else {
                                null
                            }
                        }
                        .dataFetcher("issues") { env ->
                            if (env.getArgument<Any?>("input") ==
                                listOf(
                                    mapOf(
                                        "projectId" to "100",
                                        "issueId" to "1",
                                    ),
                                    mapOf(
                                        "projectId" to "100",
                                        "issueId" to "2"
                                    ),
                                    mapOf(
                                        "projectId" to "101",
                                        "issueId" to "3",
                                    ),
                                )
                            ) {
                                listOf(
                                    MyService_Issue(id = "1", key = "GQLGW-001", projectId = "100"),
                                    MyService_Issue(id = "3", key = "BUILD-003", projectId = "101"),
                                )
                            } else {
                                null
                            }
                        }
                }
            },
        ),
    ),
) {
    private data class MyService_Issue(
        val id: String? = null,
        val projectId: String? = null,
        val key: String? = null,
        val relatedIds: List<MyService_RelatedIssue?>? = null,
    )

    private data class MyService_RelatedIssue(
        val projectId: String? = null,
        val issueId: String? = null,
    )

    private data class MyService_Sprint(
        val id: String? = null,
    )

    private data class MyService_UnderlyingIssueInput(
        val projectId: String? = null,
        val issueId: String? = null,
    )
}
