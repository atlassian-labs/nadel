package graphql.nadel.tests.next.fixtures.execution.interfaceexpansion.underlyingonly

import graphql.nadel.tests.next.NadelIntegrationTest
import org.intellij.lang.annotations.Language

/**
 * Like hiddenmembership, but `Secret` implements `Node` only in the underlying schema and is absent from the
 * overall schema entirely — the strongest form of hiding. The underlying `nodes` resolver returns a `Secret`,
 * so these characterise what a client receives for a node whose concrete type has no overall counterpart.
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

class UnderlyingOnlyImplBareInterfaceFieldTest : UnderlyingOnlyImplementationTestBase(
    query = """
        query {
          nodes {
            id
          }
        }
    """.trimIndent(),
)

class UnderlyingOnlyImplBareTypenameTest : UnderlyingOnlyImplementationTestBase(
    query = """
        query {
          nodes {
            __typename
          }
        }
    """.trimIndent(),
)
