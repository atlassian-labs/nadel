package graphql.nadel.tests.next.fixtures.execution.interfaceexpansion.hiddenmembership

import graphql.nadel.tests.next.NadelIntegrationTest
import org.intellij.lang.annotations.Language

/**
 * Characterises today's interface→object-fragment expansion. In the overall schema only `Issue`/`Story`
 * implement `Node` (`Task` is a visible object type but not a member); in the underlying schema `Task` also
 * implements `Node`. So the exposed set is a strict subset of the underlying impls, and graphql-java rewrites a
 * bare interface selection into one `... on ExposedImpl` fragment per exposed type — how an undeployed sibling
 * type ends up named in a query the client never wrote. The snapshots' `calls` block is the demonstration.
 * (For a type absent from the overall schema entirely, see the underlyingonly fixtures.)
 */
abstract class HiddenImplementationTestBase(
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
                type Task {
                  id: ID
                }
            """.trimIndent(),
            underlyingSchema = """
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
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Issue(val id: String, val issueField: String)
                data class Story(val id: String, val storyField: String)
                data class Task(val id: String)

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
                                Task(id = "TASK-1"),
                            )
                        }
                    }
            },
        ),
    ),
)

/** The core case: bare `nodes { id }` is expanded into `... on Issue`/`... on Story` downstream. */
class HiddenImplBareInterfaceFieldTest : HiddenImplementationTestBase(
    query = """
        query {
          nodes {
            id
          }
        }
    """.trimIndent(),
)

class HiddenImplBareTypenameTest : HiddenImplementationTestBase(
    query = """
        query {
          nodes {
            __typename
          }
        }
    """.trimIndent(),
)

/** Explicit `... on Issue` is honoured verbatim — the client opted into a concrete type. */
class HiddenImplExplicitExposedImplTest : HiddenImplementationTestBase(
    query = """
        query {
          nodes {
            ... on Issue {
              issueField
            }
          }
        }
    """.trimIndent(),
)

/** Naming every exposed impl normalizes to the same ENF as the bare selection — intent is lost by compile time. */
class HiddenImplExplicitAllExposedImplsTest : HiddenImplementationTestBase(
    query = """
        query {
          nodes {
            ... on Issue {
              id
            }
            ... on Story {
              id
            }
          }
        }
    """.trimIndent(),
)
