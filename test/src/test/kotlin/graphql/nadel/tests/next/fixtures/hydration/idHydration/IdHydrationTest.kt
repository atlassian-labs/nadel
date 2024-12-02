package graphql.nadel.tests.next.fixtures.hydration.idHydration

import graphql.nadel.Nadel
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.tests.next.NadelIntegrationTest

class IdHydrationTest : NadelIntegrationTest(
    query = """
        query {
          issues {
            assignee {
              name
            }
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "Jira",
            overallSchema = """
                type Query {
                  issues: [Issue]
                }
                type Issue {
                  assigneeId: ID
                  assignee: User @idHydrated(idField: "assigneeId")
                }
            """.trimIndent(),
            runtimeWiring = { runtime ->
                data class Issue(val assigneeId: String)

                runtime
                    .type("Query") { type ->
                        type
                            .dataFetcher("issues") { env ->
                                listOf(
                                    Issue(
                                        assigneeId = "ari:cloud:identity::user/1",
                                    ),
                                    Issue(
                                        assigneeId = "ari:cloud:identity::user/128",
                                    ),
                                )
                            }
                    }
            },
        ),
        Service(
            name = "Identity",
            overallSchema = """
                type Query {
                  usersByIds(ids: [ID!]!): [User]
                }
                type User @defaultHydration(field: "usersByIds", idArgument: "ids", batchSize: 1, identifiedBy: "id") {
                  id: ID!
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { runtime ->
                data class User(val id: String, val name: String)

                val usersByIds = listOf(
                    User(id = "ari:cloud:identity::user/1", name = "First"),
                    User(id = "ari:cloud:identity::user/128", name = "2^7"),
                ).strictAssociateBy { it.id }

                runtime
                    .type("Query") { type ->
                        type
                            .dataFetcher("usersByIds") { env ->
                                env.getArgument<List<String>>("ids")
                                    ?.map {
                                        usersByIds[it]
                                    }
                            }
                    }
            },
        ),
    ),
) {
    override fun makeNadel(): Nadel.Builder {
        return super.makeNadel()
            .blueprintHint { true }
    }
}
