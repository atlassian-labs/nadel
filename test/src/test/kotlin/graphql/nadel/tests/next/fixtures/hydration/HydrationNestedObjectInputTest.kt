package graphql.nadel.tests.next.fixtures.hydration

import graphql.nadel.NadelExecutionHints
import graphql.nadel.tests.next.NadelIntegrationTest

/**
 * Tests that we can hydrate using a `IssueUserSearchInput` output object feeding into `UserSearchInput` input object.
 *
 * These objects have nested objects e.g. `IssueUserSearchInput.filters` references object `IssueUserSearchFilterInput`
 *
 * We need to test that we create the appropriate selection set on the source object i.e.
 *
 * ```graphql
 * arguments: [{name: "filter", value: "$source.assigneeFilter"}]
 * ```
 *
 * Turns into
 *
 * ```graphql
 * assigneeFilter {
 *   filter {
 *     parentId
 *     subEntityTypes
 *   }
 * }
 * ```
 */
class HydrationNestedObjectInputTest : NadelIntegrationTest(
    query = """
        query {
          issues {
            id
            key
            assignee {
              id
              name
            }
          }
        }
    """.trimIndent(),
    variables = mapOf(),
    services = listOf(
        Service(
            name = "issues",
            overallSchema = """
                type Query {
                  issues: [Issue]
                }
                type Issue {
                  id: ID!
                  key: String
                  assigneeFilter: IssueUserSearchInput
                  assignee: User
                    @hydrated(
                      field: "userByFilter"
                      arguments: [{name: "filter", value: "$source.assigneeFilter"}]
                    )
                }
                type IssueUserSearchInput {
                  filter: [IssueUserSearchFilterInput!]
                }
                type IssueUserSearchFilterInput {
                  parentId: String
                  subEntityTypes: [String!]
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class IssueUserSearchFilterInput(
                    val parentId: String,
                    val subEntityTypes: List<String>?,
                )

                data class IssueUserSearchInput(
                    val filter: List<IssueUserSearchFilterInput>?,
                )

                data class Issue(
                    val id: String,
                    val key: String,
                    val assigneeFilter: IssueUserSearchInput,
                )

                val issuesById = listOf(
                    Issue(
                        id = "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1",
                        key = "GQLGW-1",
                        assigneeFilter = IssueUserSearchInput(
                            filter = listOf(
                                IssueUserSearchFilterInput(
                                    parentId = "123",
                                    subEntityTypes = listOf("a", "b", "c"),
                                ),
                            ),
                        ),
                    )
                ).associateBy { it.id }

                wiring
                    .type("Query") { type ->
                        type.dataFetcher("issues") { issuesById.values }
                    }
            },
        ),
        Service(
            name = "identity",
            overallSchema = """
                type Query {
                  userByFilter(filter: UserSearchInput!): User
                }
                input UserSearchInput {
                  filter: [UserSearchFilterInput!]
                }
                input UserSearchFilterInput {
                  parentId: String
                  subEntityTypes: [String!]
                }
                type User {
                  id: ID!
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class User(
                    val id: String,
                    val name: String,
                )

                val usersById = listOf(
                    User(
                        id = "ari:cloud:identity::user/1",
                        name = "Franklin Wang",
                    ),
                ).associateBy { it.id }

                wiring
                    .type("Query") { type ->
                        type.dataFetcher("userByFilter") {
                            // Eh, we're not going to verify the data here, the snapshot will capture the input
                            usersById.values.single()
                        }
                    }
            },
        ),
    ),
) {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints()
            .hydrationExecutableSourceFields { true }
    }
}
