package graphql.nadel.tests.next.fixtures.execution.interfaceexpansion.union

import graphql.nadel.tests.next.NadelIntegrationTest
import org.intellij.lang.annotations.Language

/**
 * # Union analogue of the same problem
 *
 * The expansion isn't interface‑specific — it happens for **unions** too. `Actor` is a union of `{User, Issue}`
 * in the overall schema but `{User, Issue, Bot}` in the underlying schema (`Bot` is a hidden member).
 *
 * A union declares no selectable data fields, so the only interface‑level selection possible is `__typename`.
 * A bare `actor { __typename }` resolves to the exposed members `{User, Issue}` — a strict subset of the
 * underlying members — so it is expanded into `... on User { __typename } ... on Issue { __typename }`,
 * naming the concrete exposed members.
 *
 * (A future "forgiving expansion" fix targets interfaces; unions are included here so the characterization is
 * complete and the team can see the same intent‑loss on the union side.)
 */
abstract class UnionHiddenMemberTestBase(
    @Language("GraphQL") query: String,
) : NadelIntegrationTest(
    query = query,
    services = listOf(
        Service(
            name = "data",
            overallSchema = """
                type Query {
                  actor: Actor
                }
                union Actor = User | Issue
                type User {
                  id: ID
                }
                type Issue {
                  id: ID
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  actor: Actor
                }
                union Actor = User | Issue | Bot
                type User {
                  id: ID
                }
                type Issue {
                  id: ID
                }
                type Bot {
                  id: ID
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class User(val id: String)

                wiring
                    .type("Actor") { type ->
                        type.typeResolver { env ->
                            env.schema.getObjectType(env.getObject<Any>().javaClass.simpleName)
                        }
                    }
                    .type("Query") { type ->
                        type.dataFetcher("actor") { _ ->
                            User(id = "USER-1")
                        }
                    }
            },
        ),
    ),
)

/**
 * Bare `actor { __typename }` over a union with a hidden member ⇒ expanded downstream into
 * `... on User { __typename } ... on Issue { __typename }` (the hidden `Bot` member is never named).
 */
class UnionHiddenMemberBareTypenameTest : UnionHiddenMemberTestBase(
    query = """
        query {
          actor {
            __typename
          }
        }
    """.trimIndent(),
)

/**
 * Contrast: an explicit `... on User` member fragment is honoured verbatim. Explicit member fragments are the
 * only way to select data fields on a union, and naming a concrete member is the client's explicit choice.
 */
class UnionHiddenMemberExplicitMemberTest : UnionHiddenMemberTestBase(
    query = """
        query {
          actor {
            ... on User {
              id
            }
          }
        }
    """.trimIndent(),
)
