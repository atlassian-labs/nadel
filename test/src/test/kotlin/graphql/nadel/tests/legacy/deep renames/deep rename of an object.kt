package graphql.nadel.tests.legacy.`deep renames`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `deep rename of an object` : NadelLegacyIntegrationTest(
    query = """
        query {
          issues {
            id
            authorName {
              firstName
              lastName
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "Issues",
            overallSchema = """
                type Query {
                  issues: [Issue]
                }
                type Issue {
                  id: ID
                  authorName: Name @renamed(from: "authorDetails.name")
                }
                type Name {
                  firstName: String @renamed(from: "fName")
                  lastName: String @renamed(from: "lName")
                }
            """.trimIndent(),
            underlyingSchema = """
                type AuthorDetail {
                  name: Name
                }
                type Issue {
                  authorDetails: AuthorDetail
                  id: ID
                }
                type Name {
                  fName: String
                  lName: String
                }
                type Query {
                  issues: [Issue]
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("issues") { env ->
                        listOf(
                            Issues_Issue(
                                authorDetails = Issues_AuthorDetail(
                                    name = Issues_Name(
                                        fName = "George",
                                        lName = "Smith",
                                    ),
                                ),
                                id = "ISSUE-1",
                            ),
                            Issues_Issue(
                                authorDetails = Issues_AuthorDetail(
                                    name = Issues_Name(
                                        fName = "Elizabeth",
                                        lName = "Windsor"
                                    )
                                ),
                                id = "ISSUE-2",
                            ),
                        )
                    }
                }
            },
        ),
    ),
) {
    private data class Issues_AuthorDetail(
        val name: Issues_Name? = null,
    )

    private data class Issues_Issue(
        val authorDetails: Issues_AuthorDetail? = null,
        val id: String? = null,
    )

    private data class Issues_Name(
        val fName: String? = null,
        val lName: String? = null,
    )
}
