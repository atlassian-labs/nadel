package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `batch hydration input is null` : NadelLegacyIntegrationTest(
    query = """
        query {
          issues {
            id
            authors {
              name
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "UserService",
            overallSchema = """
                type Query {
                  usersByIds(ids: [ID]): [User]
                }
                type User {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  usersByIds(ids: [ID]): [User]
                }
                type User {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
            },
        ),
        Service(
            name = "Issues",
            overallSchema = """
                type Query {
                  issues: [Issue]
                }
                type Issue {
                  id: ID
                  authors: [User]
                  @hydrated(
                    service: "UserService"
                    field: "usersByIds"
                    arguments: [{name: "ids" value: "${'$'}source.authorIds"}]
                    identifiedBy: "id"
                    batchSize: 2
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Issue {
                  authorIds: [ID]
                  id: ID
                }
                type Query {
                  issues: [Issue]
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("issues") { env ->
                        listOf(Issues_Issue(authorIds = null, id = "ISSUE-1"))
                    }
                }
            },
        ),
    ),
) {
    private data class UserService_User(
        val id: String? = null,
        val name: String? = null,
    )

    private data class Issues_Issue(
        val authorIds: List<String?>? = null,
        val id: String? = null,
    )
}
