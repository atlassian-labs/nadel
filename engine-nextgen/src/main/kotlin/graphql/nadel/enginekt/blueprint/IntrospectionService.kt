package graphql.nadel.enginekt.blueprint

import graphql.ExecutionInput
import graphql.GraphQL
import graphql.GraphqlErrorHelper.toSpecification
import graphql.language.AstPrinter
import graphql.nadel.Service
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionResult
import graphql.schema.GraphQLSchema

internal class IntrospectionService constructor(schema: GraphQLSchema) :
    Service(name, schema, makeServiceExecution(schema), null, null) {

    companion object {
        const val name = "__introspection"

        private fun makeServiceExecution(schema: GraphQLSchema): ServiceExecution {
            return ServiceExecution { params ->
                val executionInput = ExecutionInput.newExecutionInput()
                    .query(AstPrinter.printAstCompact(params.query))
                GraphQL.newGraphQL(schema)
                    .build()
                    .executeAsync(executionInput)
                    .thenApply { ServiceExecutionResult(it.getData(), it.errors.map(::toSpecification)) }
            }
        }
    }
}
