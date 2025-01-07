package graphql.nadel.tests.legacy.scalars

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import graphql.scalars.ExtendedScalars
import graphql.scalars.alias.AliasedScalar

class `date time scalar as output type` : NadelLegacyIntegrationTest(
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
                  aField: DateTime
                }
                scalar DateTime
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  aField: DateTime
                }
                scalar DateTime
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("aField") { env ->
                        "2022-03-09T05:01:50Z"
                    }
                }
                wiring.scalar(
                    AliasedScalar
                        .Builder()
                        .name("DateTime")
                        .aliasedScalar(ExtendedScalars.Json)
                        .build(),
                )
            },
        ),
    ),
)
