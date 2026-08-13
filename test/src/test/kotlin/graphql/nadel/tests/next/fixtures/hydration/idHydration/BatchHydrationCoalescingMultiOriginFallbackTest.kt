package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.nadel.NadelExecutionHints
import graphql.nadel.hints.NadelBatchHydrationCoalescingHint
import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.nadel.tests.next.SimpleClassNameTypeResolver

/**
 * A union can inherit equivalent defaults from several member declarations. Querying the same
 * multi-origin hydration under two aliases must retain the existing isolated execution path.
 */
class BatchHydrationCoalescingMultiOriginFallbackTest : NadelIntegrationTest(
    query = """
        query {
          issues {
            first: user {
              __typename
            }
            second: user {
              __typename
            }
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "Identity",
            overallSchema = """
                type Query {
                  issues: [Issue]
                  usersByIds(ids: [ID!]!): [User]
                }
                type Issue {
                  userId: ID @hidden
                  user: IssueUser @idHydrated(idField: "userId")
                }
                union IssueUser = CustomerUser | AtlassianAccountUser | AppUser
                interface User {
                  id: ID!
                }
                type CustomerUser implements User
                  @defaultHydration(field: "usersByIds", idArgument: "ids", batchSize: 100, identifiedBy: "id") {
                  id: ID!
                }
                type AtlassianAccountUser implements User
                  @defaultHydration(field: "usersByIds", idArgument: "ids", batchSize: 100, identifiedBy: "id") {
                  id: ID!
                }
                type AppUser implements User
                  @defaultHydration(field: "usersByIds", idArgument: "ids", batchSize: 100, identifiedBy: "id") {
                  id: ID!
                }
            """.trimIndent(),
            runtimeWiring = { runtime ->
                data class Issue(
                    val userId: String,
                )

                data class CustomerUser(
                    override val id: String,
                ) : UserValue

                data class AtlassianAccountUser(
                    override val id: String,
                ) : UserValue

                data class AppUser(
                    override val id: String,
                ) : UserValue

                val usersById = listOf<UserValue>(
                    CustomerUser(id = "ari:cloud:identity::user/customer"),
                    AtlassianAccountUser(id = "ari:cloud:identity::user/account"),
                    AppUser(id = "ari:cloud:identity::user/app"),
                ).associateBy { it.id }

                runtime
                    .type("Query") { type ->
                        type
                            .dataFetcher("issues") {
                                listOf(
                                    Issue(userId = "ari:cloud:identity::user/customer"),
                                    Issue(userId = "ari:cloud:identity::user/account"),
                                    Issue(userId = "ari:cloud:identity::user/app"),
                                )
                            }
                            .dataFetcher("usersByIds") { environment ->
                                environment.getArgument<List<String>>("ids")
                                    ?.map(usersById::get)
                            }
                    }
                    .type("User") { type ->
                        type.typeResolver(SimpleClassNameTypeResolver)
                    }
                    .type("IssueUser") { type ->
                        type.typeResolver(SimpleClassNameTypeResolver)
                    }
            },
        ),
    ),
) {
    interface UserValue {
        val id: String
    }

    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints()
            .batchHydrationCoalescing(NadelBatchHydrationCoalescingHint { _ -> true })
    }
}
