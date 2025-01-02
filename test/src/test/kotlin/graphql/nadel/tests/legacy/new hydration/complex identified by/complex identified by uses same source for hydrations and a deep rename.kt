package graphql.nadel.tests.legacy.`new hydration`.`complex identified by`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `complex identified by uses same source for hydrations and a deep rename` : NadelLegacyIntegrationTest(
    query = """
        query {
          foos {
            renamedField
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
                  foos: [Foo]
                  details(detailIds: [ID]): [Detail]
                  issues(issueIds: [ID]): [Issue]
                }
                type Foo {
                  renamedField: String @renamed(from: "issue.field")
                  issue: Issue @hydrated(
                    service: "Foo"
                    field: "issues"
                    arguments: [{name: "issueIds" value: "${'$'}source.fooId"}]
                    inputIdentifiedBy: [{sourceId: "fooId" resultId: "issueId"}]
                    batchSize: 2
                  )
                  detail: Detail @hydrated(
                    service: "Foo"
                    field: "details"
                    arguments: [{name: "detailIds" value: "${'$'}source.fooId"}]
                    inputIdentifiedBy: [{sourceId: "fooId" resultId: "detailId"}]
                    batchSize: 2
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
                  issueId: ID
                  field: String
                  fooId: ID
                }
                type Query {
                  details(detailIds: [ID]): [Detail]
                  foos: [Foo]
                  issues(issueIds: [ID]): [Issue]
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    val issuesByIds = listOf(
                        Foo_Issue(`field` = "field_name", issueId = "Foo-1"),
                        Foo_Issue(`field` = "field_name-2", issueId = "Foo-2"),
                        Foo_Issue(`field` = "field-3", issueId = "Foo-3"),
                    ).associateBy { it.issueId }
                    val detailsByIds = listOf(
                        Foo_Detail(detailId = "Foo-2", name = "Foo 2 Electric Boogaloo"),
                        Foo_Detail(detailId = "Foo-1", name = "apple"),
                        Foo_Detail(detailId = "Foo-3", name = "Three Apples"),
                    ).associateBy { it.detailId }

                    type
                        .dataFetcher("foos") { env ->
                            listOf(
                                Foo_Foo(
                                    fooId = "Foo-1",
                                    issue = Foo_Issue(`field` = "hmm-1"),
                                ),
                                Foo_Foo(
                                    fooId = "Foo-2",
                                    issue = Foo_Issue(`field` = "hmm-2"),
                                ),
                                Foo_Foo(
                                    fooId = "Foo-3",
                                    issue = Foo_Issue(`field` = "hmm-3"),
                                ),
                            )
                        }
                        .dataFetcher("issues") { env ->
                            env.getArgument<List<String>>("issueIds")?.map(issuesByIds::get)
                        }
                        .dataFetcher("details") { env ->
                            env.getArgument<List<String>>("detailIds")?.map(detailsByIds::get)
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
        val issueId: String? = null,
        val `field`: String? = null,
        val fooId: String? = null,
    )
}
