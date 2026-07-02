package graphql.nadel.tests.next.fixtures.execution.interfaceexpansion.union

import graphql.nadel.tests.next.NadelIntegrationTest
import org.intellij.lang.annotations.Language

/**
 * Union analogue: the expansion isn't interface-specific. `Actor` is `{User, Issue}` overall but
 * `{User, Issue, Bot}` underlying (`Bot` hidden). A union's only abstract-level selection is `__typename`, so a
 * bare `actors { __typename }` is expanded into `... on User`/`... on Issue`, naming the exposed members; the
 * hidden `Bot` comes back as `{}`. The hint relaxes this — see the `*RelaxedTest` counterpart.
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
                  actors: [Actor]
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
                  actors: [Actor]
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
                data class Bot(val id: String)

                wiring
                    .type("Actor") { type ->
                        type.typeResolver { env ->
                            env.schema.getObjectType(env.getObject<Any>().javaClass.simpleName)
                        }
                    }
                    .type("Query") { type ->
                        type.dataFetcher("actors") { _ ->
                            listOf(
                                User(id = "USER-1"),
                                Bot(id = "BOT-1"),
                            )
                        }
                    }
            },
        ),
    ),
)

class UnionHiddenMemberBareTypenameTest : UnionHiddenMemberTestBase(
    query = """
        query {
          actors {
            __typename
          }
        }
    """.trimIndent(),
)

/** Explicit `... on User` is honoured verbatim; the hidden `Bot` (matching no fragment) comes back as `{}`. */
class UnionHiddenMemberExplicitMemberTest : UnionHiddenMemberTestBase(
    query = """
        query {
          actors {
            ... on User {
              id
            }
          }
        }
    """.trimIndent(),
)
