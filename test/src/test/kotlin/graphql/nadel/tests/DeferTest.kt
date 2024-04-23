package graphql.nadel.tests

import graphql.incremental.DeferPayload
import graphql.incremental.IncrementalExecutionResult
import graphql.incremental.StreamPayload
import graphql.language.AstPrinter
import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionInput
import graphql.nadel.NadelSchemas
import graphql.nadel.NadelServiceExecutionResultImpl
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import kotlinx.coroutines.reactive.asFlow
import java.util.concurrent.CompletableFuture

data class Service(
    val name: String,
    val overallSchema: String,
    val underlyingSchema: String = overallSchema,
)

fun NadelSchemas(
    vararg services: Service,
    executionFactory: ServiceExecutionFactory,
): NadelSchemas {
    return NadelSchemas.newNadelSchemas()
        .overallSchemas(services.associate { it.name to it.overallSchema })
        .underlyingSchemas(services.associate { it.name to it.underlyingSchema })
        .serviceExecutionFactory(executionFactory)
        .build()
}

suspend fun main() {
    val source = "\$source"
    val schemas = NadelSchemas(
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
        ),
        Service(
            name = "issues",
            overallSchema = """
                type Query {
                    issueById(id: ID!):Issue 
                }
                type Issue {
                    id: ID!
                    assigneeId: ID! @hidden
                    assignee: User
                        @hydrated(
                            service: "identity"
                            field: "userById"
                            arguments: [{ name: "id", value: "$source.assigneeId" }]
                        )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                    issueById(id: ID!):Issue 
                }
                type Issue {
                    id: ID!
                    assigneeId: ID!
                }
            """.trimIndent(),
        ),
        executionFactory = { service ->
            when (service) {
                "identity" -> ServiceExecution {
                    println(AstPrinter.printAst(it.query))
                    CompletableFuture.completedFuture(
                        NadelServiceExecutionResultImpl(
                            data = mutableMapOf(
                                "userById" to mutableMapOf(
                                    "name" to "Franklin",
                                ),
                            ),
                        ),
                    )
                }
                "issues" -> ServiceExecution {
                    println(AstPrinter.printAst(it.query))
                    CompletableFuture.completedFuture(
                        NadelServiceExecutionResultImpl(
                            data = mutableMapOf(
                                "issueById" to mutableMapOf(
                                    "id" to "1",
                                    "hydration__assignee__assigneeId" to "ari:cloud:identity::user/fwang",
                                    "__typename__hydration__assignee" to "Issue",
                                )
                            ),
                        ),
                    )
                }
                else -> throw UnsupportedOperationException()
            }
        }
    )

    val nadel = Nadel.newNadel()
        .schemas(schemas)
        .build()

    val result = nadel.execute(
        NadelExecutionInput.Builder()
            .query(
                """
                    query {
                        issueById(id: "1") {
                            id
                            assignee {
                                name
                            }
                        }
                    }
                """.trimIndent()
            )
            .build()
    )

    println(result)

    println("Incremental results")

    (result as IncrementalExecutionResult)
        .incrementalItemPublisher
        .asFlow()
        .collect { next ->
            next.incremental
                ?.asSequence()
                ?.map {
                    when (it) {
                        is StreamPayload -> it.toSpecification()
                        is DeferPayload -> it.toSpecification()
                        else -> {}
                    }
                }
                ?.forEach {
                    println(it)
                }
        }
}
