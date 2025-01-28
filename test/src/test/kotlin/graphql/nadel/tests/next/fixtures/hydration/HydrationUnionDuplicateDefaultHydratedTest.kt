package graphql.nadel.tests.next.fixtures.hydration

import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.nadel.tests.next.SimpleClassNameTypeResolver

class HydrationUnionDuplicateDefaultHydratedTest : NadelIntegrationTest(
    query = """
        {
          issues {
            user {
              __typename
            }
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "myService",
            overallSchema = """
                type Query {
                  issues: [Issue]
                  users(ids: [ID!]!): [User]
                }
                type Issue {
                  userId: ID @hidden
                  user: IssueUser @idHydrated(idField: "userId")
                }
                union IssueUser = CustomerUser | AtlassianAccountUser | AppUser
                interface User {
                  id: ID!
                }
                type CustomerUser implements User @defaultHydration(field: "users", idArgument: "ids") {
                  id: ID!
                }
                type AtlassianAccountUser implements User @defaultHydration(field: "users", idArgument: "ids") {
                  id: ID!
                }
                type AppUser implements User @defaultHydration(field: "users", idArgument: "ids") {
                  id: ID!
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        data class Issue(val userId: String)
                        data class AtlassianAccountUser(override val id: String) : User
                        data class AppUser(override val id: String) : User
                        data class CustomerUser(override val id: String) : User

                        val usersById: Map<String, User> = listOf(
                            AtlassianAccountUser(id = "aoeu"),
                            CustomerUser(id = "asdf"),
                            AppUser(id = "bot"),
                        ).associateBy { it.id }

                        type
                            .dataFetcher("issues") { env ->
                                listOf(
                                    Issue(userId = "aoeu"),
                                    Issue(userId = "asdf"),
                                    Issue(userId = "wow"),
                                )
                            }
                            .dataFetcher("users") { env ->
                                env.getArgument<List<String>>("ids")?.map(usersById::get)
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
    interface User {
        val id: String
    }
}
