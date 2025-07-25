package graphql.nadel.tests.next.fixtures.stub

import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.nadel.tests.next.SimpleClassNameTypeResolver

class StubFieldOnInterfaceOutputTest_Id_KeyTask : StubFieldOnInterfaceOutputTest(
    query = """
        {
          issues {
            id
            ... on Task {
              key
            }
          }
        }
    """.trimIndent(),
)

class StubFieldOnInterfaceOutputTest_KeyOnTask : StubFieldOnInterfaceOutputTest(
    query = """
        {
          issues {
            ... on Task {
              key
            }
          }
        }
    """.trimIndent(),
)

class StubFieldOnInterfaceOutputTest_KeyOnIssue : StubFieldOnInterfaceOutputTest(
    query = """
        {
          issues {
            ... on Issue {
              key
            }
          }
        }
    """.trimIndent(),
)

class StubFieldOnInterfaceOutputTest_KeyOnIssueAndTask : StubFieldOnInterfaceOutputTest(
    query = """
        {
          issues {
            ... on Issue {
              key
            }
            ... on Task {
              key
            }
          }
        }
    """.trimIndent(),
)

class StubFieldOnInterfaceOutputTest_IdOnly : StubFieldOnInterfaceOutputTest(
    query = """
        {
          issues {
            id
          }
        }
    """.trimIndent(),
)

abstract class StubFieldOnInterfaceOutputTest(query: String) : NadelIntegrationTest(
    query = query,
    services = listOf(
        Service(
            name = "myService",
            overallSchema = """
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
                  key: String @stubbed
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
                  title: String
                  description: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Issue(
                    val id: String,
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
