package graphql.nadel.tests.next.fixtures.hydration

import graphql.nadel.tests.next.NadelIntegrationTest

class HydrationIdentifiedByRenamedFieldTest : NadelIntegrationTest(
    query = """
        {
          me {
            friends {
              name
            }
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "myService",
            overallSchema = """
                type Query {
                  me: Me
                  users(ids: [ID!]!): [User]
                }
                type Me {
                  friendIds: [ID!] @hidden
                  friends: [User] @idHydrated(idField: "friendIds")
                }
                type User @defaultHydration(field: "users", idArgument: "ids", identifiedBy: "id", batchSize: 90) {
                   id: ID @renamed(from: "canonicalAccountId")
                   name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Me(
                    val friendIds: List<String>,
                )

                data class User(
                    val canonicalAccountId: String,
                    val name: String,
                )

                val usersByIds = listOf(
                    User(
                        canonicalAccountId = "i",
                        name = "Imagine",
                    ),
                    User(
                        canonicalAccountId = "2i",
                        name = "Imagine 2 friends",
                    ),
                ).associateBy { it.canonicalAccountId }

                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("me") { env ->
                                Me(
                                    friendIds = listOf("i", "2i"),
                                )
                            }
                            .dataFetcher("users") { env ->
                                env.getArgument<List<String>>("ids")?.map(usersByIds::get)
                            }
                    }
            },
        ),
    ),
)
