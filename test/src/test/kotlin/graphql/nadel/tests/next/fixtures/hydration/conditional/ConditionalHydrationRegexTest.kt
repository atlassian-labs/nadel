package graphql.nadel.tests.next.fixtures.hydration.conditional

import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.tests.next.NadelIntegrationTest

// ari:cloud:jira:TEST--67b8ae80-cf1a-4de7-b77c-3f8ff51977b3:issue/10000
class ConditionalHydrationRegexTest : NadelIntegrationTest(
    query = """
        query {
          search {
            object {
              __typename
            }
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "search",
            overallSchema = """
                directive @synthetic on UNION
                type Query {
                  search: [SearchResult]
                }
                type SearchResult {
                  objectId: ID!
                  object: SearchResultObject
                    @hydrated(
                      service: "issues"
                      field: "issueById"
                      arguments: [{name: "id", value: "$source.objectId"}]
                      when: {
                        result: {
                          sourceField: "objectId"
                          predicate: {
                            matches: "^ari:cloud:jira:[^:]+:issue/.+$"
                          }
                        }
                      }
                    )
                    @hydrated(
                      service: "identity"
                      field: "userById"
                      arguments: [{name: "id", value: "$source.objectId"}]
                      when: {
                        result: {
                          sourceField: "objectId"
                          predicate: {
                            matches: "^ari:cloud:identity::user/.+$"
                          }
                        }
                      }
                    )
                }
                union SearchResultObject @synthetic = Issue | User
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class SearchResult(val objectId: String)

                val results = listOf(
                    SearchResult(
                        objectId = "ari:cloud:jira:TEST--67b8ae80-cf1a-4de7-b77c-3f8ff51977b3:issue/10000",
                    ),
                    SearchResult(
                        objectId = "ari:cloud:identity::user/1",
                    ),
                )

                wiring
                    .type("Query"){type->
                        type
                            .dataFetcher("search"){
                                results
                            }
                    }
            },
        ),
        Service(
            name = "issues",
            overallSchema = """
                type Query {
                  issueById(id: ID!): Issue
                }
                type Issue {
                  id: ID!
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Issue(val id: String)

                val issuesById = listOf(
                    Issue(
                        id = "ari:cloud:jira:TEST--67b8ae80-cf1a-4de7-b77c-3f8ff51977b3:issue/10000",
                    ),
                ).strictAssociateBy { it.id }

                wiring
                    .type("Query") { type ->
                        type.dataFetcher("issueById") { env ->
                            issuesById[env.getArgument<String>("id")]
                        }
                    }
            },
        ),
        Service(
            name = "identity",
            overallSchema = """
                type Query {
                  userById(id: ID!): User
                }
                type User {
                  id: ID!
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class User(val id: String)

                val usersById = listOf(
                    User(
                        id = "ari:cloud:identity::user/1",
                    ),
                ).strictAssociateBy { it.id }

                wiring
                    .type("Query") { type ->
                        type.dataFetcher("userById") { env ->
                            usersById[env.getArgument<String>("id")]
                        }
                    }
            },
        ),
    )
)
