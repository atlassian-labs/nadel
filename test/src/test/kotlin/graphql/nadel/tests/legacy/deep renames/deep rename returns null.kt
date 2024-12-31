package graphql.nadel.tests.legacy.`deep renames`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `deep rename returns null` : NadelLegacyIntegrationTest(
    query = """
        query {
          troll {
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
                  troll: Troll
                }
                type Troll {
                  name: String @renamed(from: "firstEat.item.name")
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  troll: Troll
                }
                type Troll {
                  id: ID
                  firstEat: EatLog
                }
                type EatLog {
                  id: ID
                  item: Edible
                }
                type Edible {
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("troll") { env ->
                        Issues_Troll(firstEat = Issues_EatLog(item = null))
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
        val name: String? = null,
    )

    private data class Issues_Troll(
        val id: String? = null,
        val firstEat: Issues_EatLog? = null,
    )
}
