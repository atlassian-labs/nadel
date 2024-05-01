package graphql.nadel.tests

import graphql.ExecutionInput
import graphql.GraphQL
import graphql.incremental.DeferPayload
import graphql.incremental.IncrementalExecutionResult
import graphql.incremental.StreamPayload
import graphql.language.AstPrinter
import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionInput
import graphql.nadel.NadelSchemas
import graphql.nadel.NadelServiceExecutionResultImpl
import graphql.nadel.ServiceExecution
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import kotlinx.coroutines.reactive.asFlow

data class Service(
    val name: String,
    val overallSchema: String,
    val underlyingSchema: String = overallSchema,
    val runtimeWiring: RuntimeWiring,
)

fun NadelSchemas(
    vararg services: Service,
): NadelSchemas {
    val underlyingSchemas = services.associate { it.name to it.underlyingSchema }

    return NadelSchemas.newNadelSchemas()
        .overallSchemas(services.associate { it.name to it.overallSchema })
        .underlyingSchemas(underlyingSchemas)
        .serviceExecutionFactory { serviceName ->
            val service = services
                .single { it.name == serviceName }
            val graphQL = GraphQL
                .newGraphQL(
                    SchemaGenerator()
                        .makeExecutableSchema(
                            SchemaParser().parse(service.underlyingSchema),
                            service.runtimeWiring,
                        ),
                )
                .build()

            ServiceExecution { params ->
                graphQL
                    .executeAsync(
                        ExecutionInput.newExecutionInput()
                            .query(AstPrinter.printAst(params.query))
                            .variables(params.variables)
                            .build()
                    )
                    .thenApply {
                        val spec = it.toSpecification()

                        NadelServiceExecutionResultImpl(
                            data = spec["data"] as MutableMap<String, Any?>? ?: mutableMapOf(),
                            errors = spec["errors"] as MutableList<MutableMap<String, Any?>>? ?: mutableListOf(),
                            extensions = spec["extensions"] as MutableMap<String, Any?>? ?: mutableMapOf(),
                        )
                    }
            }
        }
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
            runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .type("Query") { builder ->
                    builder
                        .dataFetcher("userById") { env ->
                            mutableMapOf(
                                "id" to env.getArgument("id"),
                                "name" to "Franklin Wang",
                            )
                        }
                }
                .build(),
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
            runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .type("Query") { builder ->
                    builder
                        .dataFetcher("issueById") { env ->
                            mutableMapOf(
                                "id" to env.getArgument("id"),
                                "assigneeId" to "ari:cloud:identity::user/fwang",
                            )
                        }
                }
                .build(),
        ),
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
                                id
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
