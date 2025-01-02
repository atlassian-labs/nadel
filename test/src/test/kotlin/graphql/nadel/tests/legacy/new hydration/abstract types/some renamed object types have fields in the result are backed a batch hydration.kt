package graphql.nadel.tests.legacy.`new hydration`.`abstract types`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `some renamed object types have fields in the result are backed a batch hydration` :
    NadelLegacyIntegrationTest(
        query = """
            {
              activity {
                user {
                  name
                }
              }
            }
        """.trimIndent(),
        variables = emptyMap(),
        services =
        listOf(
            Service(
                name = "activity",
                overallSchema = """
                    type Query {
                      activity: [IActivity]
                    }
                    interface IActivity {
                      user: User
                    }
                    type Activity implements IActivity {
                      id: ID!
                      user: User
                      @hydrated(
                        service: "users"
                        field: "usersByIds"
                        arguments: [{name: "ids" value: "${'$'}source.userId"}]
                        inputIdentifiedBy: [{sourceId: "userId" resultId: "id"}]
                      )
                    }
                    type SingleActivity implements IActivity {
                      id: ID!
                      user: User
                    }
                """.trimIndent(),
                underlyingSchema = """
                    type Query {
                      activity: [IActivity]
                    }
                    type User {
                      id: ID!
                      name: String
                    }
                    interface IActivity {
                      user: User
                    }
                    type Activity implements IActivity {
                      id: ID!
                      userId: ID
                      user: User @deprecated(reason: "Fake")
                    }
                    type SingleActivity implements IActivity {
                      id: ID!
                      user: User
                    }
                """.trimIndent(),
                runtimeWiring = { wiring ->
                    wiring.type("Query") { type ->
                        type.dataFetcher("activity") { env ->
                            listOf(
                                Activity_SingleActivity(user = Activity_User(name = "John")),
                                Activity_Activity(userId = "user-100"),
                                Activity_Activity(userId = "user-20"),
                                Activity_SingleActivity(user = Activity_User(name = "Mayor")),
                            )
                        }
                    }
                    wiring.type("IActivity") { type ->
                        type.typeResolver { typeResolver ->
                            val obj = typeResolver.getObject<Any>()
                            val typeName = obj.javaClass.simpleName.substringAfter("_")
                            typeResolver.schema.getTypeAs(typeName)
                        }
                    }
                },
            ),
            Service(
                name = "users",
                overallSchema = """
                    type Query {
                      usersByIds(ids: [ID!]!): [User]
                    }
                    type User {
                      id: ID!
                      name: String
                    }
                """.trimIndent(),
                underlyingSchema = """
                    type Query {
                      usersByIds(ids: [ID!]!): [User]
                    }
                    type User {
                      id: ID!
                      name: String
                    }
                """.trimIndent(),
                runtimeWiring = { wiring ->
                    wiring.type("Query") { type ->
                        type.dataFetcher("usersByIds") { env ->
                            if (env.getArgument<Any?>("ids") == listOf("user-100", "user-20")) {
                                listOf(
                                    Users_User(id = "user-100", name = "Spaces"),
                                    Users_User(
                                        id = "user-20",
                                        name =
                                        "Newmarket",
                                    ),
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
    private data class Activity_Activity(
        val id: String? = null,
        val userId: String? = null,
        override val user: Activity_User? = null,
    ) : Activity_IActivity

    private interface Activity_IActivity {
        val user: Activity_User?
    }

    private data class Activity_SingleActivity(
        val id: String? = null,
        override val user: Activity_User? = null,
    ) : Activity_IActivity

    private data class Activity_User(
        val id: String? = null,
        val name: String? = null,
    )

    private data class Users_User(
        val id: String? = null,
        val name: String? = null,
    )
}
