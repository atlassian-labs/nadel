package graphql.nadel.tests.next.fixtures.execution.interfaceexpansion.underlyingonly

import graphql.nadel.tests.next.NadelIntegrationTest
import org.intellij.lang.annotations.Language

/**
 * # Truly underlying‑only interface implementation
 *
 * Contrast with the `hiddenmembership` fixtures, where the extra `Node` implementation (`Task`) is still a
 * declared, client‑visible object type in the overall schema — it simply doesn't implement `Node` there.
 *
 * Here `Secret` implements `Node` in the **underlying** schema and is **not present in the overall schema at
 * all** — no type mapping, nothing a client could ever name. This is the strongest form of information hiding
 * and the most direct model of the problem (a sibling implementation the backend knows about but the
 * client‑facing schema does not).
 *
 * These tests characterise whether Nadel even *accepts* such a schema, what it sends downstream for a bare
 * interface‑level selection, and — the interesting part — what the client receives for a node whose concrete
 * type has **no overall counterpart** (the underlying `nodes` resolver deliberately returns a `Secret`).
 */
abstract class UnderlyingOnlyImplementationTestBase(
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
                type Secret implements Node {
                  id: ID
                  secretField: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Issue(val id: String, val issueField: String)
                data class Story(val id: String, val storyField: String)
                data class Secret(val id: String, val secretField: String)

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
                                Secret(id = "SECRET-1", secretField = "shhh"),
                            )
                        }
                    }
            },
        ),
    ),
)

/**
 * Bare interface‑level selection with a truly underlying‑only implementation (`Secret`). The downstream query
 * still expands to `... on Issue { id } ... on Story { id }` (the exposed impls), so `Secret` is never named.
 * The captured result shows what the client receives for the returned `Secret` node.
 */
class UnderlyingOnlyImplBareInterfaceFieldTest : UnderlyingOnlyImplementationTestBase(
    query = """
        query {
          nodes {
            id
          }
        }
    """.trimIndent(),
)

/**
 * Bare `__typename` with a truly underlying‑only implementation. The captured result shows whether/how the
 * `Secret` node's concrete type (which has no overall counterpart) surfaces to the client.
 */
class UnderlyingOnlyImplBareTypenameTest : UnderlyingOnlyImplementationTestBase(
    query = """
        query {
          nodes {
            __typename
          }
        }
    """.trimIndent(),
)
