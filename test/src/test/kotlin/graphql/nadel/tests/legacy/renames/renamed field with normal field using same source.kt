package graphql.nadel.tests.legacy.renames

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `renamed field with normal field using same source` : NadelLegacyIntegrationTest(
    query = """
        query {
          foo {
            issue {
              fooDetail {
                name
              }
            }
            renamedField
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
                }
                type Foo {
                  renamedField: String @renamed(from: "issue.field")
                  issue: Issue
                }
                type Issue {
                  fooDetail: Detail
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
                  issue: Issue
                }
                type Issue {
                  field: String
                  fooDetail: Detail
                }
                type Query {
                  detail(detailIds: [ID]): Detail
                  foo: Foo
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("foo") { env ->
                        Foo_Foo(issue = Foo_Issue(fooDetail = Foo_Detail(name = "fooName"), `field` = "field"))
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
        val issue: Foo_Issue? = null,
    )

    private data class Foo_Issue(
        val `field`: String? = null,
        val fooDetail: Foo_Detail? = null,
    )
}
