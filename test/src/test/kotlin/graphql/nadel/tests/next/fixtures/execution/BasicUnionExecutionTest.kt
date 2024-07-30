package graphql.nadel.tests.next.fixtures.execution

import graphql.nadel.tests.next.NadelIntegrationTest
import org.intellij.lang.annotations.Language

class TypenameInUnionSelectionTest : BasicUnionExecutionTestBase(
    query = """
        query {
          abstract {
            __typename
          }
        }
    """.trimIndent(),
)

class PartialUnionTypeConditionsUnionTest : BasicUnionExecutionTestBase(
    query = """
        query {
          abstract {
            ... on Issue {
              __typename
              key
            }
          }
        }
    """.trimIndent(),
)

class AllUnionTypeConditionsUnionTest : BasicUnionExecutionTestBase(
    query = """
        query {
          abstract {
            ... on Issue {
              __typename
              key
            }
            ... on User {
              name
            }
          }
        }
    """.trimIndent(),
)


class UnionAliasToSameResultKeyTest : BasicUnionExecutionTestBase(
    query = """
        query {
          abstract {
            __typename
            ... on Issue {
              id: key
            }
            ... on User {
              id: name
            }
          }
        }
    """.trimIndent(),
)

abstract class BasicUnionExecutionTestBase(
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
