package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `same source for  hydrations` : NadelLegacyIntegrationTest(
    query = """
        query {
          foo {
            issue {
              field
            }
            detail {
              name
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
                  detail(detailId: ID): Detail
                  issue(issueId: ID): Issue
                }
                type Foo {
                  issue: Issue
                  @hydrated(
                    service: "Foo"
                    field: "issue"
                    arguments: [{name: "issueId" value: "${'$'}source.fooId"}]
                  )
                  detail: Detail
                  @hydrated(
                    service: "Foo"
                    field: "detail"
                    arguments: [{name: "detailId" value: "${'$'}source.fooId"}]
                  )
                }
                type Detail {
                  detailId: ID!
                  name: String
                }
                type Issue {
                  fooId: ID
                  field: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Detail {
                  detailId: ID!
                  name: String
                }
                type Foo {
                  field: String
                  fooId: ID
                  issue: Issue
                }
                type Issue {
                  field: String
                  fooId: ID
                }
                type Query {
                  detail(detailId: ID): Detail
                  foo: Foo
                  issue(issueId: ID): Issue
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type
                        .dataFetcher("foo") { env ->
                            Foo_Foo(fooId = "ID")
                        }.dataFetcher("issue") { env ->
                            if (env.getArgument<Any?>("issueId") == "ID") {
                                Foo_Issue(`field` = "field_name")
                            } else {
                                null
                            }
                        }.dataFetcher("detail") { env ->
                            if (env.getArgument<Any?>("detailId") == "ID") {
                                Foo_Detail(name = "apple")
                            } else {
                                null
                            }
                        }
                }
            },
        ),
    ),
) {
    private data class Foo_Detail(
        val detailId: String? = null,
        val name: String? = null,
    )

    private data class Foo_Foo(
        val `field`: String? = null,
        val fooId: String? = null,
        val issue: Foo_Issue? = null,
    )

    private data class Foo_Issue(
        val `field`: String? = null,
        val fooId: String? = null,
    )
}
