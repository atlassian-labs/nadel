package graphql.nadel.tests.next.fixtures.execution.interfaceexpansion.hiddenmembership

import graphql.nadel.tests.next.NadelIntegrationTest
import org.intellij.lang.annotations.Language

/**
 * # Interface → object‑fragment expansion (CURRENT behaviour)
 *
 * These tests characterise what Nadel sends downstream **today**. There is no fix here; the generated
 * `*Snapshot.kt` files ARE the demonstration — read the `calls` block (the query Nadel sent to the
 * underlying service) against the `Query` shown in each snapshot's KDoc (the query the *client* sent).
 *
 * ## The problem
 *
 * `Node` is an interface. In the **overall** (client‑facing) schema only `Issue` and `Story` implement it;
 * `Task` is a declared, client‑visible object type there but is **not** a `Node` member. In the **underlying**
 * schema `Task` *also* implements `Node`. So the object type itself is visible overall — what's hidden is its
 * `Node` *membership* — which still makes the interface's exposed implementations `{Issue, Story}` a strict
 * subset of the underlying implementations `{Issue, Story, Task}`. (For an implementation whose *type* is
 * absent from the overall schema entirely, see the `underlyingonly` fixtures.)
 *
 * When a client selects an interface field at the **interface level** (e.g. `nodes { id }`, no
 * `... on ConcreteType`), normalization resolves the field's object types to the *exposed* set
 * `{Issue, Story}`. Because that is a **strict subset** of the underlying implementations
 * `{Issue, Story, Task}`, graphql‑java's compiler decides the field is "conditional" and **rewrites the
 * bare selection into one `... on ExposedImpl` inline fragment per exposed implementation**.
 *
 * That rewrite is the problem: a newly‑added/undeployed sibling implementation gets named in the downstream
 * query even though the client never asked for it, and a backend that doesn't know that type yet rejects the
 * whole operation with `UnknownType`.
 *
 * The client's "I only asked at the interface level" intent is gone by compile time — a bare selection and an
 * explicit‑all‑exposed selection normalize to the *same* thing ([HiddenImplExplicitAllExposedImplsTest]).
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

/**
 * ⭐ **The core problem.** Bare interface‑level selection of an interface‑declared field.
 *
 * Client sends `nodes { id }`. Downstream, Nadel expands it into `... on Issue { id } ... on Story { id }` —
 * naming the concrete exposed types even though the client named none. (The hidden `Task` node comes back as
 * `{}` because it was never queried.)
 */
class HiddenImplBareInterfaceFieldTest : HiddenImplementationTestBase(
    query = """
        query {
          nodes {
            id
          }
        }
    """.trimIndent(),
)

/**
 * Bare `__typename` at the interface level is expanded the same way — `... on Issue { __typename }
 * ... on Story { __typename }` — because the exposed set is still a strict subset of the underlying impls.
 */
class HiddenImplBareTypenameTest : HiddenImplementationTestBase(
    query = """
        query {
          nodes {
            __typename
          }
        }
    """.trimIndent(),
)

/**
 * Contrast: the client wrote an **explicit** `... on Issue` fragment. This is honoured verbatim — only
 * `... on Issue { issueField }` is sent, `Story`/`Task` are never queried. Explicit object fragments are the
 * client opting into naming a concrete type, so today's behaviour is already correct and must be preserved.
 */
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

/**
 * The crux of why intent is lost. The client explicitly named **every exposed implementation**
 * (`... on Issue { id } ... on Story { id }`). This normalizes to the *identical* `ExecutableNormalizedField`
 * as the bare [HiddenImplBareInterfaceFieldTest] — so the downstream query is byte‑identical. Post‑normalization
 * there is no way to tell "asked at the interface level" from "named every exposed object type."
 */
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
