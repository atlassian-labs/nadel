package graphql.nadel.engine.blueprint

import graphql.ExecutionInput
import graphql.GraphQL
import graphql.GraphqlErrorHelper.toSpecification
import graphql.language.AstPrinter
import graphql.language.OperationDefinition
import graphql.nadel.NadelServiceExecutionResultImpl
import graphql.nadel.NadelTypeDefinitionRegistry
import graphql.nadel.Service
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionParameters
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.engine.util.toBuilder
import graphql.nadel.engine.util.toBuilderWithoutTypes
import graphql.nadel.util.NamespacedUtil.isNamespacedField
import graphql.schema.DataFetchingEnvironment
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import java.util.concurrent.CompletableFuture

internal class IntrospectionService(
    schema: GraphQLSchema,
    introspectionRunnerFactory: NadelIntrospectionRunnerFactory,
) : Service(name, schema, introspectionRunnerFactory.make(schema), NadelTypeDefinitionRegistry()) {
    companion object {
        const val name = "__introspection"
    }
}

fun interface NadelIntrospectionRunnerFactory {
    fun make(schema: GraphQLSchema): ServiceExecution
}

open class NadelDefaultIntrospectionRunner(schema: GraphQLSchema) : ServiceExecution {
    protected val graphQL: GraphQL = GraphQL
        .newGraphQL(injectNamespaceDataFetchers(schema))
        .build()

    override fun execute(serviceExecutionParameters: ServiceExecutionParameters): CompletableFuture<ServiceExecutionResult> {
        if (serviceExecutionParameters.operationDefinition.operation == OperationDefinition.Operation.SUBSCRIPTION) {
            return CompletableFuture.completedFuture(NadelServiceExecutionResultImpl())
        }
        return graphQL
            .executeAsync(
                ExecutionInput.newExecutionInput()
                    .query(AstPrinter.printAstCompact(serviceExecutionParameters.query))
                    .variables(serviceExecutionParameters.variables)
                    .also(::makeExecutionInput)
                    .build()
            )
            .thenApply {
                NadelServiceExecutionResultImpl(
                    data = it.getData() ?: mutableMapOf(),
                    errors = it.errors.mapTo(ArrayList(), ::toSpecification),
                )
            }
    }

    protected open fun makeExecutionInput(input: ExecutionInput.Builder) {
    }

    companion object {
        internal fun injectNamespaceDataFetchers(schema: GraphQLSchema): GraphQLSchema {
            val codeRegistryWithNewDataFetchers = schema.codeRegistry.toBuilder()
                .also { builder ->
                    // This inserts data fetchers for namespaced fields so we can handle their __typename internally
                    getFieldsWithCoordinates(schema.queryType, schema.mutationType, schema.subscriptionType)
                        .filter { (_, field) ->
                            isNamespacedField(field)
                        }
                        .forEach { (coordinates) ->
                            builder.dataFetcher(coordinates) { _: DataFetchingEnvironment ->
                                // Returning anything here is fine
                                return@dataFetcher Unit
                            }
                        }
                }
                .build()

            return schema.toBuilderWithoutTypes()
                .codeRegistry(codeRegistryWithNewDataFetchers)
                .build()
        }

        private fun getFieldsWithCoordinates(
            vararg types: GraphQLObjectType?,
        ): Sequence<Pair<FieldCoordinates, GraphQLFieldDefinition>> {
            return types
                .asSequence()
                .filterNotNull()
                .flatMap { type ->
                    type.fields
                        .asSequence()
                        .map { field ->
                            makeFieldCoordinates(type.name, field.name) to field
                        }
                }
        }
    }
}
