package graphql.nadel.tests.next.fixtures.execution.interfaceexpansion.union

import graphql.nadel.tests.next.NadelIntegrationTest
import org.intellij.lang.annotations.Language

/**
 * Union analogue: `Actor` is `{User, Issue}` overall but `{User, Issue, Bot}` underlying (`Bot` hidden). A union's
 * only abstract-level selection is `__typename`.
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
