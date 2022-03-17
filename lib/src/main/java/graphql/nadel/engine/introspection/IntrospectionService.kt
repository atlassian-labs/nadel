package graphql.nadel.engine.introspection

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

internal object IntrospectionService {
    const val name = "__introspection"
}

internal object IntrospectionServiceFactory {
    fun make(schema: GraphQLSchema, runnerFactory: NadelIntrospectionRunnerFactory): Service {
        return Service(
            name = IntrospectionService.name,
            underlyingSchema = schema,
            serviceExecution = runnerFactory.make(schema),
            definitionRegistry = NadelDefinitionRegistry(),
        )
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
                    .build()
            )
            .thenApply {
                ServiceExecutionResult(
                    data = it.getData(),
                    errors = it.errors.mapTo(ArrayList(), ::toSpecification),
                )
            }
    }
}
