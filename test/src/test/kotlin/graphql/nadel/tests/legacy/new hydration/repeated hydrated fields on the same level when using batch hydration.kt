package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `repeated hydrated fields on the same level when using batch hydration` : NadelLegacyIntegrationTest(
    query = """
        query {
          foo {
            issue {
              name
            }
            issue {
              desc
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "Foo",
            overallSchema = """
                type Query {
                  foo: Foo
                  issues(issueIds: [ID!]): [Issue!]
                }
                type Foo {
                  issue: Issue
                  @hydrated(
                    service: "Foo"
                    field: "issues"
                    arguments: [{name: "issueIds" value: "${'$'}source.issueId"}]
                  )
                }
                type Issue {
                  id: ID
                  name: String
                  desc: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Foo {
                  issueId: ID
                }
                type Issue {
                  desc: String
                  id: ID
                  name: String
                }
                type Query {
                  foo: Foo
                  issues(issueIds: [ID!]): [Issue!]
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type
                        .dataFetcher("foo") { env ->
                            Foo_Foo(issueId = "ISSUE-1")
                        }.dataFetcher("issues") { env ->
                            if (env.getArgument<Any?>("issueIds") == listOf("ISSUE-1")) {
                                listOf(Foo_Issue(desc = "I AM A DESC", id = "ISSUE-1", name = "I AM A NAME"))
                            } else {
                                null
                            }
                        }
                }
            },
        ),
    ),
) {
    private data class Foo_Foo(
        val issueId: String? = null,
    )

    private data class Foo_Issue(
        val desc: String? = null,
        val id: String? = null,
        val name: String? = null,
    )
}
