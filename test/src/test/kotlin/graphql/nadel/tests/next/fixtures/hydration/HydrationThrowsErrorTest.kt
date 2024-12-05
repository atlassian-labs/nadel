package graphql.nadel.tests.next.fixtures.hydration

import graphql.nadel.Nadel
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.error.NadelGraphQLErrorException
import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.normalized.ExecutableNormalizedField

class HydrationThrowsErrorTest : NadelIntegrationTest(
    query = """
        query {
          issueById(id: "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1") {
            id
            key
            assignee {
              id
              name
            }
          }
        }
    """.trimIndent(),
    variables = mapOf(),
    services = listOf(
        Service(
            name = "issues",
            overallSchema = """
                type Query {
                  issueById(id: ID!): Issue
                }
                type Issue {
                  id: ID!
                  key: String
                  assigneeId: ID @hidden
                  assignee: User
                    @hydrated(
                      service: "identity"
                      field: "userById"
                      arguments: [{name: "id", value: "$source.assigneeId"}]
                    )
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Issue(
                    val id: String,
                    val key: String,
                    val assigneeId: String? = null,
                )

                val issuesById = listOf(
                    Issue(
                        id = "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1",
                        key = "GQLGW-1",
                        assigneeId = "ari:cloud:identity::user/1",
                    )
                ).associateBy { it.id }

                wiring
                    .type("Query") { type ->
                        type.dataFetcher("issueById") {
                            issuesById[it.getArgument("id")]
                        }
                    }
            },
        ),
        Service(
            name = "identity",
            overallSchema = """
                type Query {
                  userById(id: ID!): User
                }
                type User {
                  id: ID!
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
            },
        ),
    ),
) {
    override fun makeNadel(): Nadel.Builder {
        return super.makeNadel()
            .transforms(
                listOf(
                    ThrowErrorTransform(),
                ),
            )
    }

    private class ThrowErrorTransform : NadelTransform<Any> {
        override suspend fun isApplicable(
            executionContext: NadelExecutionContext,
            serviceExecutionContext: NadelServiceExecutionContext,
            executionBlueprint: NadelOverallExecutionBlueprint,
            services: Map<String, graphql.nadel.Service>,
            service: graphql.nadel.Service,
            overallField: ExecutableNormalizedField,
            hydrationDetails: ServiceExecutionHydrationDetails?,
        ): Any? {
            if (overallField.name == "userById") {
                throw Bye()
            }

            return null
        }

        override suspend fun getResultInstructions(
            executionContext: NadelExecutionContext,
            serviceExecutionContext: NadelServiceExecutionContext,
            executionBlueprint: NadelOverallExecutionBlueprint,
            service: graphql.nadel.Service,
            overallField: ExecutableNormalizedField,
            underlyingParentField: ExecutableNormalizedField?,
            result: ServiceExecutionResult,
            state: Any,
            nodes: JsonNodes,
        ): List<NadelResultInstruction> {
            throw UnsupportedOperationException()
        }

        override suspend fun transformField(
            executionContext: NadelExecutionContext,
            serviceExecutionContext: NadelServiceExecutionContext,
            transformer: NadelQueryTransformer,
            executionBlueprint: NadelOverallExecutionBlueprint,
            service: graphql.nadel.Service,
            field: ExecutableNormalizedField,
            state: Any,
        ): NadelTransformFieldResult {
            throw UnsupportedOperationException()
        }
    }

    class Bye : NadelGraphQLErrorException(
        message = "BYE",
    )
}
