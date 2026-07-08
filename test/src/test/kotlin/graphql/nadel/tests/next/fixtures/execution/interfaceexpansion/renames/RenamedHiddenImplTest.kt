package graphql.nadel.tests.next.fixtures.execution.interfaceexpansion.renames

import graphql.nadel.tests.next.NadelIntegrationTest
import org.intellij.lang.annotations.Language

/**
 * Relaxation combined with a RENAMED exposed implementation. `Node` is implemented overall by `JiraIssue`
 * (renamed from underlying `Issue`) and `Story`; the underlying schema additionally has `Secret`, which is not
 * exposed. So a bare `Node` selection resolves to `{JiraIssue, Story}`, a strict subset of the underlying
 * `{Issue, Story, Secret}` - the hiding condition - while an exposed member is also type-renamed. The other
 * interfaceexpansion fixtures use only plain (un-renamed) types; this exercises the underlying<->overall type
 * mapping on both the query side (bare emission) and the result side (hiding + the `__typename` the client
 * gets back is the overall name `JiraIssue`).
 */
abstract class RenamedHiddenImplTestBase(
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
                type JiraIssue implements Node @renamed(from: "Issue") {
                  id: ID
                }
                type Story implements Node {
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
                    .type("Query") { type ->
                        type.dataFetcher("nodes") { _ ->
                            listOf(
                                Issue(id = "ISSUE-1"),
                                Story(id = "STORY-1"),
                                Secret(id = "SECRET-1"),
                            )
                        }
                    }
            },
        ),
    ),
)

class RenamedHiddenImplBareInterfaceFieldTest : RenamedHiddenImplTestBase(
    query = """
        query {
          nodes {
            id
          }
        }
    """.trimIndent(),
)

class RenamedHiddenImplBareTypenameTest : RenamedHiddenImplTestBase(
    query = """
        query {
          nodes {
            __typename
          }
        }
    """.trimIndent(),
)
