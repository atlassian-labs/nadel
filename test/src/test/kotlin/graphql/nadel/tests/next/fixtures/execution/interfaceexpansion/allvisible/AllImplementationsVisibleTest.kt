package graphql.nadel.tests.next.fixtures.execution.interfaceexpansion.allvisible

import graphql.nadel.tests.next.NadelIntegrationTest
import org.intellij.lang.annotations.Language

/**
 * Control: Issue/Story/Task all implement Node in both schemas, so nothing is hidden and a bare interface
 * selection is already sent bare — with or without the hint. Contrast the hiddenmembership/underlyingonly fixtures.
 */
abstract class AllImplementationsVisibleTestBase(
    @Language("GraphQL") query: String,
) : NadelIntegrationTest(
    query = query,
    services = listOf(
        Service(
            name = "data",
            overallSchema = """
                type Query {
                  nodes: [Node]
                }
                interface Node {
                  id: ID
                }
                type Issue implements Node {
                  id: ID
                  issueField: String
                }
                type Story implements Node {
                  id: ID
                  storyField: String
                }
                type Task implements Node {
                  id: ID
                  taskField: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Issue(val id: String, val issueField: String)
                data class Story(val id: String, val storyField: String)
                data class Task(val id: String, val taskField: String)

                wiring
                    .type("Node") { type ->
                        type.typeResolver { env ->
                            env.schema.getObjectType(env.getObject<Any>().javaClass.simpleName)
                        }
                    }
                    .type("Query") { type ->
                        type.dataFetcher("nodes") { _ ->
                            listOf(
                                Issue(id = "ISSUE-1", issueField = "i"),
                                Story(id = "STORY-1", storyField = "s"),
                                Task(id = "TASK-1", taskField = "t"),
                            )
                        }
                    }
            },
        ),
    ),
)

class AllVisibleBareInterfaceFieldTest : AllImplementationsVisibleTestBase(
    query = """
        query {
          nodes {
            id
          }
        }
    """.trimIndent(),
)

class AllVisibleBareTypenameTest : AllImplementationsVisibleTestBase(
    query = """
        query {
          nodes {
            __typename
          }
        }
    """.trimIndent(),
)
