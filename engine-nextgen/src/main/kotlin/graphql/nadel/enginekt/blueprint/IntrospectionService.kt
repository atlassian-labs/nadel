package graphql.nadel.enginekt.blueprint

import graphql.ExecutionInput
import graphql.GraphQL
import graphql.GraphqlErrorHelper.toSpecification
import graphql.language.AstPrinter
import graphql.nadel.Service
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionParameters
import graphql.nadel.ServiceExecutionResult
import graphql.schema.GraphQLSchema
import java.util.concurrent.CompletableFuture

internal class IntrospectionService constructor(
    schema: GraphQLSchema,
    introspectionRunnerFactory: NadelIntrospectionRunnerFactory,
) : Service(name, schema, introspectionRunnerFactory.make(schema), null) {
    companion object {
        const val name = "__introspection"
    }
}

fun interface NadelIntrospectionRunnerFactory {
    fun make(schema: GraphQLSchema): ServiceExecution
}

open class NadelDefaultIntrospectionRunner(schema: GraphQLSchema) : ServiceExecution {
    private val graphQL = GraphQL.newGraphQL(schema).build()

    override fun execute(params: ServiceExecutionParameters): CompletableFuture<ServiceExecutionResult> {
        return graphQL
            .executeAsync(
                ExecutionInput.newExecutionInput()
                    .query(AstPrinter.printAstCompact(params.query))
                    .build()
            )
            .thenApply {
                ServiceExecutionResult(
                    it.getData(),
                    it.errors.map(::toSpecification),
                )
            }
    }
}
