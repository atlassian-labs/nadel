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
            email
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "myService",
            overallSchema = """
                type Query {
                  myWork: [WorkItem]
                  issue: WorkItem
                  task: WorkItem
                }
                interface WorkItem {
                  key: String
                }
                type Issue implements WorkItem {
                  key: String
                  whatsTheIssue: String
                }
                type Task implements WorkItem {
                  key: String @stubbed
                  whatTodo: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  myWork: [WorkItem]
                  issue: WorkItem
                  task: WorkItem
                }
                interface WorkItem {
                  key: String
                }
                type Issue implements WorkItem {
                  key: String
                  whatsTheIssue: String
                }
                type Task implements WorkItem {
                  key: String
                  whatTodo: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                abstract class WorkItem

                data class Task(
                    val key: String? = null,
                    val whatTodo: String,
                ) : WorkItem()

                data class Issue(
                    val key: String,
                    val whatsTheIssue: String,
                ) : WorkItem()

                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("myWork") { env ->
                                listOf(
                                    Task(
                                        key = "iunno",
                                        whatTodo = "Implement task key",
                                    ),
                                    Issue(
                                        key = "HELLO",
                                        whatsTheIssue = "Nothing",
                                    ),
                                    Issue(
                                        key = "BYE",
                                        whatsTheIssue = "Nothing",
                                    ),
                                    null,
                                    null,
                                    null,
                                )
                            }
                            .dataFetcher("task") { env ->
                                Task(
                                    key = "iunno",
                                    whatTodo = "Implement task key",
                                )
                            }
                            .dataFetcher("issue") { env ->
                                Task(
                                    key = "iunno",
                                    whatTodo = "SIKE",
                                )
                            }
                    }
                    .type("Task") { type ->
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
