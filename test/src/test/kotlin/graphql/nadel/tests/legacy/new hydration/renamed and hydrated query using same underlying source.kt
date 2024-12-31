package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `renamed and hydrated query using same underlying source` : NadelLegacyIntegrationTest(
    query = """
        query {
          foo {
            renamedField
            details {
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
                  detail(detailId: ID): Detail
                  foo: Foo
                }
                type Foo {
                  renamedField: String @renamed(from: "issue.field")
                  details: [Detail]
                  @hydrated(
                    service: "Foo"
                    field: "detail"
                    arguments: [{name: "detailId" value: "${'$'}source.issue.fooId"}]
                  )
                }
                type Detail {
                  detailId: ID!
                  name: String
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
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type
                        .dataFetcher("foo") { env ->
                            Foo_Foo(issue = Foo_Issue(`field` = "field", fooId = "ID"))
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
