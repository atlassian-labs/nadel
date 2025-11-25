package graphql.nadel.tests.next.fixtures.hydration

import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.nadel.tests.next.SimpleClassNameTypeResolver
import graphql.nadel.tests.next.fixtures.hydration.HydrationNonBatchedMultipleSourceArgumentsConditionalTest.QueryError
import graphql.nadel.tests.next.fixtures.hydration.HydrationNonBatchedMultipleSourceArgumentsConditionalTest.User
import graphql.schema.DataFetcher

class HydrationNonBatchedMultipleSourceArgumentsConditionalTest : NadelIntegrationTest(
    query = """
        {
          issues {
            user {
              __typename
              ... on AtlassianAccountUser {
                atlassianAccountUserId: id
              }
              ... on AppUser {
                appUserId: id
              }
              ... on CustomerUser {
                customerUserId: id
              }
              ... on QueryError {
                message
              }
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
                  customerUser(siteId: ID!, userId: ID!): CustomerUserResult
                  atlassianAccountUser(siteId: ID!, userId: ID!): AtlassianAccountUserResult
                  appUser(siteId: ID!, userId: ID!): AppUserResult
                }
                union CustomerUserResult = CustomerUser | QueryError
                union AtlassianAccountUserResult = AtlassianAccountUser | QueryError
                union AppUserResult = AppUser | QueryError
                type Issue {
                  id: ID!
                  siteId: ID @hidden
                  userId: ID @hidden
                  userType: String
                  user: IssueUser
                    @hydrated(
                      service: "myService"
                      field: "customerUser"
                      arguments: [{name: "siteId", value: "$source.siteId"}, {name: "userId", value: "$source.userId"}]
                      when: { result: { sourceField: "userType", predicate: {equals: "CUSTOMER_USER" } } }
                    )
                    @hydrated(
                      service: "myService"
                      field: "atlassianAccountUser"
                      arguments: [{name: "siteId", value: "$source.siteId"}, {name: "userId", value: "$source.userId"}]
                      when: { result: { sourceField: "userType", predicate: {equals: "ATLASSIAN_ACCOUNT_USER" } } }
                    )
                    @hydrated(
                      service: "myService"
                      field: "appUser"
                      arguments: [{name: "siteId", value: "$source.siteId"}, {name: "userId", value: "$source.userId"}]
                      when: { result: { sourceField: "userType", predicate: {equals: "APP_USER" } } }
                    )
                }
                union IssueUser = CustomerUser | AtlassianAccountUser | AppUser | QueryError
                type QueryError {
                  message: String
                }
                interface User {
                  id: ID!
                }
                type CustomerUser implements User {
                  id: ID!
                }
                type AtlassianAccountUser implements User {
                  id: ID!
                }
                type AppUser implements User {
                  id: ID!
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                val usersById: Map<String, List<User>> = listOf(
                    AtlassianAccountUser(id = "aoeu", siteId = "site1"),
                    CustomerUser(id = "asdf", siteId = "site2"),
                    AppUser(id = "bot", siteId = "site1"),
                ).groupBy { it.id }

                val issues = listOf(
                    Issue(id = "1", siteId = "site1", userId = "aoeu", userType = "ATLASSIAN_ACCOUNT_USER"),
                    Issue(id = "2", siteId = "site2", userId = "asdf", userType = "CUSTOMER_USER"),
                    Issue(id = "3", siteId = "site1", userId = "wow", userType = "APP_USER"),
                    Issue(id = "4", siteId = "site2", userId = "wow", userType = "APP_USER"),
                    Issue(id = "5", siteId = "site2", userId = "aoeu", userType = "APP_USER"),
                )

                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("issues") { issues }
                            .dataFetcher("customerUser", userDf<CustomerUser>(usersById))
                            .dataFetcher("atlassianAccountUser", userDf<AtlassianAccountUser>(usersById))
                            .dataFetcher("appUser", userDf<AppUser>(usersById))
                    }
                    .type("User") { type ->
                        type.typeResolver(SimpleClassNameTypeResolver)
                    }
                    .type("IssueUser") { type ->
                        type.typeResolver(SimpleClassNameTypeResolver)
                    }
                    .type("CustomerUserResult") { type ->
                        type.typeResolver(SimpleClassNameTypeResolver)
                    }
                    .type("AtlassianAccountUserResult") { type ->
                        type.typeResolver(SimpleClassNameTypeResolver)
                    }
                    .type("AppUserResult") { type ->
                        type.typeResolver(SimpleClassNameTypeResolver)
                    }
            },
        ),
    ),
) {
    interface User {
        val id: String
        val siteId: String
    }

    data class Issue(val id: String, val siteId: String, val userId: String, val userType: String)
    data class AtlassianAccountUser(override val id: String, override val siteId: String) : User
    data class AppUser(override val id: String, override val siteId: String) : User
    data class CustomerUser(override val id: String, override val siteId: String) : User
    data class QueryError(val message: String)
}

private inline fun <reified T : User> userDf(usersById: Map<String, List<User>>): DataFetcher<Any> {
    return DataFetcher { env ->
        val siteId = env.getArgument<String>("siteId")!!
        val userId = env.getArgument<String>("userId")!!
        val usersWithId = usersById[userId] ?: emptyList()
        if (usersWithId.isEmpty()) {
            QueryError("Could not find user $userId on site $siteId")
        } else {
            val match = usersWithId.firstOrNull { it is T && it.siteId == siteId }
            if (match != null) {
                match
            } else {
                val usersOnWrongSite = usersWithId.firstOrNull { it is T }
                if (usersOnWrongSite != null) {
                    QueryError("Found user $userId but they belong on different site than requested")
                } else {
                    QueryError("Found user $userId but the user is not a ${T::class.simpleName}")
                }
            }
        }
    }
}
