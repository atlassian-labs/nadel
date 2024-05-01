package graphql.nadel.tests.next.fixtures

import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.nadel.tests.next.SerializedJsonValue
import graphql.nadel.tests.next.jsonDataFetcher
import graphql.nadel.tests.jsonObjectMapper

class BasicObjectSchemaTest : NadelIntegrationTest(
    query = """
        query {
          issueById(id: "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1") {
            id
          }
        }
    """.trimIndent(),
    variables = mapOf(),
    services = listOf(
        Service(
            name = "",
            overallSchema = """
                type Query {
                  issueById(id: ID!): Issue
                }
                type Issue {
                  id: ID!
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") {
                        it.jsonDataFetcher("issueById") {
                            val idSerialized = jsonObjectMapper.writeValueAsString(it.getArgument("id"))

                            SerializedJsonValue.JsonObject(
                                """
                                    {
                                      "id": $idSerialized
                                    }
                                """.trimIndent(),
                            )
                        }
                    }
            },
        ),
    ),
)
