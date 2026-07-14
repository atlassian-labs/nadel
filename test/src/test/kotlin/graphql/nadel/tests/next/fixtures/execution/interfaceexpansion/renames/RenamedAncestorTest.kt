package graphql.nadel.tests.next.fixtures.execution.interfaceexpansion.renames

import graphql.nadel.tests.next.NadelIntegrationTest
import org.intellij.lang.annotations.Language

/**
 * The relaxable interface selection sits UNDER a renamed ancestor: `Container.things` is renamed from the
 * underlying `nodes`. Renames emit an artificial alias for the ancestor downstream, so the relaxed field's
 * overall query path differs from its underlying query path. This is the case a query-path-keyed bare signal
 * would silently miss; the identity-keyed signal must still emit the field bare. `Node` has a hidden
 * underlying-only impl (`Secret`) so relaxation applies.
 */
abstract class RenamedAncestorTestBase(
    @Language("GraphQL") query: String,
) : NadelIntegrationTest(
    query = query,
    services = listOf(
        Service(
            name = "data",
            overallSchema = """
                type Query {
                  container: Container
                }
                type Container {
                  things: [Node] @renamed(from: "nodes")
                }
                interface Node {
                  id: ID
                }
                type Issue implements Node {
                  id: ID
                }
                type Story implements Node {
                  id: ID
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  container: Container
                }
                type Container {
                  nodes: [Node]
                }
                interface Node {
                  id: ID
                }
                type Issue implements Node {
                  id: ID
                }
                type Story implements Node {
                  id: ID
                }
                type Secret implements Node {
                  id: ID
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Issue(val id: String)
                data class Story(val id: String)
                data class Secret(val id: String)

                wiring
                    .type("Node") { type ->
                        type.typeResolver { env ->
                            env.schema.getObjectType(env.getObject<Any>().javaClass.simpleName)
                        }
                    }
                    .type("Container") { type ->
                        type.dataFetcher("nodes") { _ ->
                            listOf(
                                Issue(id = "ISSUE-1"),
                                Story(id = "STORY-1"),
                                Secret(id = "SECRET-1"),
                            )
                        }
                    }
                    .type("Query") { type ->
                        type.dataFetcher("container") { _ -> Any() }
                    }
            },
        ),
    ),
)

class RenamedAncestorBareInterfaceFieldTest : RenamedAncestorTestBase(
    query = """
        query {
          container {
            things {
              id
            }
          }
        }
    """.trimIndent(),
)

class RenamedAncestorBareTypenameTest : RenamedAncestorTestBase(
    query = """
        query {
          container {
            things {
              __typename
            }
          }
        }
    """.trimIndent(),
)
