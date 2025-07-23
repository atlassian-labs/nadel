package graphql.nadel.tests.next.fixtures.stub

import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.nadel.tests.next.SimpleClassNameTypeResolver

class StubInterfaceFieldTest : NadelIntegrationTest(
    query = """
        {
          myWork {
            key
          }
          issue {
            key
          }
          task {
            key
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "myService",
            overallSchema = """
                type Query {
                  myWork: [WorkItem]
                  issue: Task
                  task: Issue
                }
                interface WorkItem {
                  id: ID!
                  key: String @stubbed
                }
                type Issue implements WorkItem {
                  id: ID!
                  key: String @stubbed
                  whatsTheIssue: String
                }
                type Task implements WorkItem {
                  id: ID!
                  key: String @stubbed
                  whatTodo: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  myWork: [WorkItem]
                  issue: Task
                  task: Issue
                }
                interface WorkItem {
                  id: ID!
                }
                type Issue implements WorkItem {
                  id: ID!
                  whatsTheIssue: String
                }
                type Task implements WorkItem {
                  id: ID!
                  whatTodo: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                abstract class WorkItem

                data class Task(
                    val id: String,
                    val whatTodo: String,
                ) : WorkItem()

                data class Issue(
                    val id: String,
                    val whatsTheIssue: String,
                ) : WorkItem()

                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("myWork") { env ->
                                listOf(
                                    Task(
                                        id = "1",
                                        whatTodo = "Implement task key",
                                    ),
                                    Issue(
                                        id = "2",
                                        whatsTheIssue = "Nothing",
                                    ),
                                    Issue(
                                        id = "3",
                                        whatsTheIssue = "Nothing",
                                    ),
                                    null,
                                    null,
                                    null,
                                )
                            }
                            .dataFetcher("task") { env ->
                                Issue(
                                    id = "4",
                                    whatsTheIssue = "Implement task key",
                                )
                            }
                            .dataFetcher("issue") { env ->
                                Task(
                                    id = "5",
                                    whatTodo = "SIKE",
                                )
                            }
                    }
                    .type("Task") { type ->
                        type.dataFetcher("key") {
                            throw UnsupportedOperationException("Not implemented")
                        }
                    }
                    .type("Issue") { type ->
                        type.dataFetcher("key") {
                            throw UnsupportedOperationException("Not implemented")
                        }
                    }
                    .type("WorkItem") { type ->
                        type.typeResolver(SimpleClassNameTypeResolver)
                    }
            },
        ),
    ),
)
