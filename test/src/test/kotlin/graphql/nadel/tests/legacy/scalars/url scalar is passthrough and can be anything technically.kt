package graphql.nadel.tests.legacy.scalars

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import graphql.scalars.ExtendedScalars
import graphql.scalars.alias.AliasedScalar

class `url scalar is passthrough and can be anything technically` : NadelLegacyIntegrationTest(
    query = """
        query {
          aField
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "service",
            overallSchema = """
                type Query {
                  aField: URL
                }
                scalar URL
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  aField: URL
                }
                scalar URL
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("aField") { env ->
                        "KFC Good"
                    }
                }
                wiring.scalar(
                    AliasedScalar
                        .Builder()
                        .name("URL")
                        .aliasedScalar(ExtendedScalars.Json)
                        .build(),
                )
            },
        ),
    ),
)
