package graphql.nadel.tests.next.fixtures.stub

import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.nadel.tests.next.SimpleClassNameTypeResolver

class StubOneInterfaceImplementationFieldTest : NadelIntegrationTest(
    query = """
        query {
          issues {
            id
            ... on Issue {
              key
            }
            ... on Task {
              key
            }
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "myService",
            overallSchema = """
                directive @stubbed on FIELD_DEFINITION
                type Query {
                  issues: [IssueLike]
                }
                interface IssueLike {
                  id: ID!
                }
                type Task implements IssueLike {
                  id: ID!
                  key: String @stubbed
                  task: String
                }
                type Issue implements IssueLike {
                  id: ID!
                  key: String
                  title: String
                  description: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  issues: [IssueLike]
                }
                interface IssueLike {
                  id: ID!
                }
                type Task implements IssueLike {
                  id: ID!
                  task: String
                }
                type Issue implements IssueLike {
                  id: ID!
                  key: String
                  title: String
                  description: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Issue(
                    val id: String,
                    val key: String,
                    val title: String,
                    val description: String,
                )

                data class Task(
                    val id: String,
                    val task: String,
                )

                wiring
                    .type("IssueLike") {
                        it.typeResolver(SimpleClassNameTypeResolver)
                    }
                    .type("Query") { type ->
                        type
                            .dataFetcher("issues") { env ->
                                listOf(
                                    Issue(
                                        id = "123",
                                        title = "Wow an issue",
                                        key = "Wow",
                                        description = "Stop procrastinating and do the work",
                                    ),
                                    null,
                                    Task(
                                        id = "456",
                                        task = "Say hello",
                                    ),
                                )
                            }
                    }
            },
        ),
    ),
)
