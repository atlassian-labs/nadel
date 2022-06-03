package graphql.nadel.engine.blueprint

import graphql.ExecutionInput
import graphql.GraphQL
import graphql.GraphqlErrorHelper.toSpecification
import graphql.language.AstPrinter
import graphql.nadel.NadelDefinitionRegistry
import graphql.nadel.Service
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionParameters
import graphql.nadel.ServiceExecutionResult
import graphql.schema.GraphQLSchema
import java.util.concurrent.CompletableFuture

internal class IntrospectionService constructor(
    schema: GraphQLSchema,
    introspectionRunnerFactory: NadelIntrospectionRunnerFactory,
) : Service(name, schema, introspectionRunnerFactory.make(schema), NadelDefinitionRegistry()) {
    companion object {
        const val name = "__introspection"
    }
}

fun interface NadelIntrospectionRunnerFactory {
    fun make(schema: GraphQLSchema): ServiceExecution
}

open class NadelDefaultIntrospectionRunner(schema: GraphQLSchema) : ServiceExecution {
    private val graphQL = GraphQL.newGraphQL(schema).build()

    override fun execute(serviceExecutionParameters: ServiceExecutionParameters): CompletableFuture<ServiceExecutionResult> {
        return graphQL
            .executeAsync(
                ExecutionInput.newExecutionInput()
                    .query(AstPrinter.printAstCompact(serviceExecutionParameters.query))
                    .variables(serviceExecutionParameters.variables)
                    .build()
            )
            .thenApply {
                ServiceExecutionResult(
                    data = it.getData() ?: mutableMapOf(),
                    errors = it.errors.mapTo(ArrayList(), ::toSpecification),
                )
            }
    }
}
