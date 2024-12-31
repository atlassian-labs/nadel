package graphql.nadel.tests.legacy.`deep renames`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `deep rename with interfaces asking typename` : NadelLegacyIntegrationTest(
    query = """
        query {
          names {
            __typename
            name
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "Issues",
            overallSchema = """
                type Query {
                  names: [HasName]
                }
                type JiraIssue implements HasName @renamed(from: "Issue") {
                  name: String
                }
                interface HasName {
                  name: String
                }
                type User implements HasName {
                  name: String @renamed(from: "details.firstName")
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  names: [HasName]
                }
                interface HasName {
                  name: String
                }
                type Issue implements HasName {
                  name: String
                }
                type UserDetails implements HasName {
                  name: String
                  firstName: String
                }
                type User implements HasName {
                  name: String
                  id: ID
                  details: UserDetails
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("names") { env ->
                        listOf(
                            Issues_Issue(name = "GQLGW-001"),
                            Issues_Issue(name = "GQLGW-1102"),
                            Issues_User(details = Issues_UserDetails(firstName = "Franklin")),
                        )
                    }
                }
                wiring.type("HasName") { type ->
                    type.typeResolver { typeResolver ->
                        val obj = typeResolver.getObject<Any>()
                        val typeName = obj.javaClass.simpleName.substringAfter("_")
                        typeResolver.schema.getTypeAs(typeName)
                    }
                }
            },
        ),
    ),
) {
    private interface Issues_HasName {
        val name: String?
    }

    private data class Issues_Issue(
        override val name: String? = null,
    ) : Issues_HasName

    private data class Issues_User(
        override val name: String? = null,
        val id: String? = null,
        val details: Issues_UserDetails? = null,
    ) : Issues_HasName

    private data class Issues_UserDetails(
        override val name: String? = null,
        val firstName: String? = null,
    ) : Issues_HasName
}
