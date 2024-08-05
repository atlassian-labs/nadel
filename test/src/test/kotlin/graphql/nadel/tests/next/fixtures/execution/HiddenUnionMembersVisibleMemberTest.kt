package graphql.nadel.tests.next.fixtures.execution

import graphql.nadel.tests.next.NadelIntegrationTest
import org.intellij.lang.annotations.Language

class HiddenUnionMembersTypenameTest : HiddenUnionMembersTestBase(
    query = """
        query {
          abstract {
            __typename
          }
        }
    """.trimIndent(),
)

class HiddenUnionMembersInlineFragmentTest : HiddenUnionMembersTestBase(
    query = """
        query {
          abstract {
            ... on Issue {
              key
            }
          }
        }
    """.trimIndent(),
)

class HiddenUnionMembersNamedFragmentTest : HiddenUnionMembersTestBase(
    query = """
        query {
          abstract {
            ...Frag
          }
        }
        fragment Frag on Issue {
          key
        }
    """.trimIndent(),
)

class HiddenUnionMembersVisibleMemberTest : HiddenUnionMembersTestBase(
    query = """
        query {
          abstract {
            ... on User {
              name
            }
          }
        }
    """.trimIndent(),
)

abstract class HiddenUnionMembersTestBase(
    @Language("GraphQL") query: String,
) : NadelIntegrationTest(
    query = query,
    services = listOf(
        Service(
            name = "abstract",
            overallSchema = """
                type Query {
                  abstract: [Abstract]
                }
                union Abstract = User
                type User {
                  name: String
                }
                type Issue {
                  key: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  abstract: [Abstract]
                }
                union Abstract = User | Issue
                type User {
                  name: String
                }
                type Issue {
                  key: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class User(
                    val name: String,
                )

                data class Issue(
                    val key: String,
                )

                wiring
                    .type("Abstract") { type ->
                        type
                            .typeResolver { env ->
                                env.schema.getObjectType(env.getObject<Any>().javaClass.simpleName)
                            }
                    }
                    .type("Query") { type ->
                        type
                            .dataFetcher("abstract") { env ->
                                listOf(
                                    User(
                                        name = "Hello",
                                    ),
                                    Issue(
                                        key = "HEL",
                                    ),
                                )
                            }
                    }
            },
        ),
    ),
)
