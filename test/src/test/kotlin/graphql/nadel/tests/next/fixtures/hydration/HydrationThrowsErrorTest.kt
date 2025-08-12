package graphql.nadel.tests.next.fixtures.hydration

import graphql.nadel.Nadel
import graphql.nadel.engine.NadelOperationExecutionContext
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldContext
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformOperationContext
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

    data class TransformOperationContext(
        override val parentContext: NadelOperationExecutionContext,
    ) : NadelTransformOperationContext()

    data class TransformFieldContext(
        override val parentContext: TransformOperationContext,
        override val overallField: ExecutableNormalizedField,
    ) : NadelTransformFieldContext<TransformOperationContext>()

    private class ThrowErrorTransform : NadelTransform<TransformOperationContext, TransformFieldContext> {
        override suspend fun getTransformOperationContext(
            operationExecutionContext: NadelOperationExecutionContext,
        ): TransformOperationContext {
            return TransformOperationContext(operationExecutionContext)
        }

        override suspend fun getTransformFieldContext(
            transformContext: TransformOperationContext,
            overallField: ExecutableNormalizedField,
        ): TransformFieldContext? {
            if (overallField.name == "userById") {
                throw Bye()
            }

            return null
        }

        override suspend fun transformField(
            transformContext: TransformFieldContext,
            transformer: NadelQueryTransformer,
            field: ExecutableNormalizedField,
        ): NadelTransformFieldResult {
            throw UnsupportedOperationException()
        }

        override suspend fun transformResult(
            transformContext: TransformFieldContext,
            underlyingParentField: ExecutableNormalizedField?,
            resultNodes: JsonNodes,
        ): List<NadelResultInstruction> {
            throw UnsupportedOperationException()
        }
    }

    class Bye : NadelGraphQLErrorException(
        message = "BYE",
    )
}
