package graphql.nadel.tests.legacy.`deep renames`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `deep rename with more interfaces` : NadelLegacyIntegrationTest(
    query = """
        query {
          names {
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
                type Edible implements HasName {
                  name: String
                }
                type Troll implements HasName {
                  # Trolls are typically named after whatever they first attempted to eat
                  name: String @renamed(from: "firstEat.item.name")
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
                type Troll implements HasName {
                  id: ID
                  name: String
                  firstEat: EatLog
                }
                type EatLog {
                  id: ID
                  item: Edible
                }
                type Edible implements HasName {
                  name: String
                }
                type Issue implements HasName {
                  name: String
                }
                type UserDetails {
                  firstName: String
                }
                type User implements HasName {
                  id: ID
                  name: String
                  details: UserDetails
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("names") { env ->
                        listOf(
                            Issues_Issue(name = "GQLGW-001"),
                            Issues_Issue(name = "GQLGW-1102"),
                            Issues_Troll(firstEat = Issues_EatLog(item = Issues_Edible(name = "My Arm"))),
                            Issues_User(details = Issues_UserDetails(firstName = "Franklin")),
                            Issues_Edible(
                                name = "Spaghetti",
                            ),
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
    private data class Issues_EatLog(
        val id: String? = null,
        val item: Issues_Edible? = null,
    )

    private data class Issues_Edible(
        override val name: String? = null,
    ) : Issues_HasName

    private interface Issues_HasName {
        val name: String?
    }

    private data class Issues_Issue(
        override val name: String? = null,
    ) : Issues_HasName

    private data class Issues_Troll(
        val id: String? = null,
        override val name: String? = null,
        val firstEat: Issues_EatLog? = null,
    ) : Issues_HasName

    private data class Issues_User(
        val id: String? = null,
        override val name: String? = null,
        val details: Issues_UserDetails? = null,
    ) : Issues_HasName

    private data class Issues_UserDetails(
        val firstName: String? = null,
    )
}
