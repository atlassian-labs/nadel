package graphql.nadel.tests.next.fixtures.execution.interfaceexpansion.allvisible

import graphql.nadel.tests.next.NadelIntegrationTest
import org.intellij.lang.annotations.Language

/**
 * # Control: no hidden implementations ⇒ no expansion
 *
 * Same `Node` interface, but here `Issue`, `Story` and `Task` **all** implement it in *both* the overall and
 * underlying schemas — nothing is hidden. A bare interface‑level selection's object types then cover *all*
 * underlying implementations, so graphql‑java emits the field **bare** downstream (it isn't conditional).
 *
 * This is the boundary of the problem: interface→object‑fragment expansion only happens when the exposed set
 * is a *strict subset* of the underlying implementations (i.e. something is hidden — see the
 * `hiddenmembership` and `underlyingonly` fixtures). With nothing hidden there is nothing to expand.
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

/**
 * Bare interface‑level selection with every implementation visible ⇒ Nadel sends `nodes { id }` **bare**
 * downstream (no `... on ConcreteType`). Contrast with the `hiddenmembership` `HiddenImplBareInterfaceFieldTest`,
 * where the identical client query is expanded because an implementation is hidden.
 */
class AllVisibleBareInterfaceFieldTest : AllImplementationsVisibleTestBase(
    query = """
        query {
          nodes {
            id
          }
        }
    """.trimIndent(),
)

/**
 * Bare `__typename` with every implementation visible ⇒ sent **bare** (`nodes { __typename }`), because it
 * covers all underlying implementations. Contrast with the `hiddenmembership` `HiddenImplBareTypenameTest`,
 * where a hidden implementation forces the same selection to expand into a fragment per exposed impl.
 */
class AllVisibleBareTypenameTest : AllImplementationsVisibleTestBase(
    query = """
        query {
          nodes {
            __typename
          }
        }
    """.trimIndent(),
)
