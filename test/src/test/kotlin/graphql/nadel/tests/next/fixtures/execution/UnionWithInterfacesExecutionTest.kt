package graphql.nadel.tests.next.fixtures.execution

import graphql.nadel.tests.next.NadelIntegrationTest

class UnionWithInterfacesExecutionTest : NadelIntegrationTest(
    query = """
        query {
          union {
            __typename
            ... on Interface {
              id
            }
            ... on User {
              name
            }
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "abstract",
            overallSchema = """
                type Query {
                  union: [Union]
                }
                interface Interface {
                  id: ID!
                }
                union Union = User | Issue
                type User implements Interface {
                  id: ID!
                  name: String
                }
                type Issue {
                  id: ID!
                  key: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class User(
                    val id: String,
                    val name: String,
                )

                data class Issue(
                    val id: String,
                    val key: String,
                )

                wiring
                    .type("Union") { type ->
                        type
                            .typeResolver { env ->
                                env.schema.getObjectType(env.getObject<Any>().javaClass.simpleName)
                            }
                    }
                    .type("Interface") { type ->
                        type
                            .typeResolver { env ->
                                env.schema.getObjectType(env.getObject<Any>().javaClass.simpleName)
                            }
                    }
                    .type("Query") { type ->
                        type
                            .dataFetcher("union") { env ->
                                listOf(
                                    User(
                                        id = "user/1",
                                        name = "Hello",
                                    ),
                                    Issue(
                                        id = "issue/1",
                                        key = "HEL",
                                    ),
                                )
                            }
                    }
            },
        ),
    ),
)
