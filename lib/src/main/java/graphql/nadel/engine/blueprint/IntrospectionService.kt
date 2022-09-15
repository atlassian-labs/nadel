package graphql.nadel.engine.blueprint

import graphql.GraphQLContext
import graphql.cachecontrol.CacheControl
import graphql.execution.ExecutionId
import graphql.execution.ExecutionStepInfo
import graphql.execution.MergedField
import graphql.execution.directives.QueryDirectives
import graphql.introspection.Introspection
import graphql.language.Document
import graphql.language.Field
import graphql.language.FragmentDefinition
import graphql.language.OperationDefinition
import graphql.nadel.NadelDefinitionRegistry
import graphql.nadel.Service
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionParameters
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.util.AnyIterable
import graphql.nadel.engine.util.AnyList
import graphql.nadel.engine.util.MutableJsonMap
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.engine.util.newServiceExecutionResult
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import graphql.schema.DataFetchingFieldSelectionSet
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLType
import graphql.schema.PropertyDataFetcher
import org.dataloader.DataLoader
import org.dataloader.DataLoaderRegistry
import java.util.Locale
import java.util.concurrent.CompletableFuture
import graphql.introspection.IntrospectionDataFetcher as GraphQLIntrospectionDataFetcher

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

typealias IntrospectionDataFetcher = GraphQLIntrospectionDataFetcher<*>

open class NadelDefaultIntrospectionRunner(
    private val schema: GraphQLSchema,
) : ServiceExecution {
    private val dataFetchers: Map<FieldCoordinates, IntrospectionDataFetcher> = run {
        Introspection::class.java.getDeclaredField("introspectionDataFetchers")
            .also {
                it.isAccessible = true
            }
            .get(null)
            .let { original ->
                original as Map<FieldCoordinates, IntrospectionDataFetcher>
                original.toMutableMap()
                    .also { copy ->
                        // todo: add other system coordinate ones
                        copy[makeFieldCoordinates(schema.queryType.name, "__schema")] =
                            Introspection.SchemaMetaFieldDefDataFetcher
                        copy[makeFieldCoordinates(schema.queryType.name, "__type")] =
                            Introspection.TypeMetaFieldDefDataFetcher
                        copy[makeFieldCoordinates(schema.queryType.name, "__typename")] =
                            GraphQLIntrospectionDataFetcher<Any> {
                                schema.queryType.name
                            }

                        also {
                            val coords = makeFieldCoordinates("__Type", "kind")
                            val defaultImpl = copy[coords]
                                ?: throw IllegalStateException("No __Type.kind data fetcher")
                            copy[coords] =
                                GraphQLIntrospectionDataFetcher<Any> { env ->
                                    defaultImpl.get(env)?.toString()
                                }
                        }

                        also {
                            val coords = makeFieldCoordinates("__Directive", "locations")
                            val defaultImpl = copy[coords]
                                ?: throw IllegalStateException("No __Directive.locations data fetcher")
                            copy[coords] =
                                GraphQLIntrospectionDataFetcher<Any> { env ->
                                    defaultImpl.get(env)
                                        .let { value ->
                                            if (value is AnyList) {
                                                value.map {
                                                    it.toString()
                                                }
                                            } else {
                                                value
                                            }
                                        }
                                }
                        }
                    }
            }
    }

    private val propertyDfs = mutableMapOf<FieldCoordinates, DataFetcher<Any?>>()

    override fun execute(serviceExecutionParameters: ServiceExecutionParameters): CompletableFuture<ServiceExecutionResult> {
        return CompletableFuture.completedFuture(
            gottaGoFast(serviceExecutionParameters.executableNormalizedField)
        )
    }

    private fun gottaGoFast(
        field: ExecutableNormalizedField,
    ): ServiceExecutionResult {
        val data: MutableJsonMap = LinkedHashMap()
        data[field.name] = getValue(field, schema)
        return newServiceExecutionResult(data = data)
    }

    private fun getValue(
        field: ExecutableNormalizedField,
        parentValue: Any?,
    ): Any? {
        val dataFetcher = getDataFetcher(field)

        val value = dataFetcher.get(
            StubDataFetchingEnvironment(
                schema,
                field,
                parentValue
            )
        )

        return evaluateValue(field, value)
    }

    private fun getDataFetcher(
        field: ExecutableNormalizedField,
    ): DataFetcher<Any?> {
        val coords = makeFieldCoordinates(
            typeName = field.objectTypeNames.single(),
            fieldName = field.name,
        )

        return (dataFetchers[coords] as DataFetcher<Any?>?)
            ?: propertyDfs.computeIfAbsent(coords) {
                PropertyDataFetcher(field.name)
            }
    }

    private fun evaluateValue(
        field: ExecutableNormalizedField,
        value: Any?,
    ): Any? {
        return when {
            value == null -> null

            // Array
            value is AnyIterable -> {
                value
                    .map {
                        evaluateValue(field, value = it)
                    }
            }

            // Object
            field.children.isNotEmpty() -> {
                getObjectValue(field.children, value)
            }

            else -> {
                value
            }
        }
    }

    private fun getObjectValue(
        children: List<ExecutableNormalizedField>,
        value: Any?,
    ): Any {
        val data: MutableJsonMap = LinkedHashMap(children.size)
        children.forEach { child ->
            data[child.name] = getValue(field = child, parentValue = value)
        }
        return data
    }
}

class StubDataFetchingEnvironment(
    private val schema: GraphQLSchema,
    private val field: ExecutableNormalizedField,
    private val source: Any?,
) : DataFetchingEnvironment {
    override fun <T : Any?> getSource(): T {
        return source as T
    }

    override fun getArguments(): MutableMap<String, Any> {
        return field.resolvedArguments
    }

    override fun getGraphQLSchema(): GraphQLSchema {
        return schema
    }

    override fun <T : Any?> getArgument(name: String): T? {
        return field.resolvedArguments[name] as T?
    }

    override fun getParentType(): GraphQLType {
        throw UnsupportedOperationException()
    }

    override fun containsArgument(name: String): Boolean {
        return field.resolvedArguments.contains(name)
    }

    override fun <T : Any?> getArgumentOrDefault(name: String, defaultValue: T): T? {
        return if (containsArgument(name)) {
            getArgument<T>(name)
        } else {
            defaultValue
        }
    }

    override fun <T : Any?> getContext(): T {
        throw UnsupportedOperationException()
    }

    override fun getGraphQlContext(): GraphQLContext {
        return GraphQLContext.getDefault()
    }

    override fun <T : Any?> getLocalContext(): T {
        throw UnsupportedOperationException()
    }

    override fun <T : Any?> getRoot(): T {
        throw UnsupportedOperationException()
    }

    override fun getFieldDefinition(): GraphQLFieldDefinition {
        throw UnsupportedOperationException()
    }

    override fun getFields(): MutableList<Field> {
        throw UnsupportedOperationException()
    }

    override fun getMergedField(): MergedField {
        throw UnsupportedOperationException()
    }

    override fun getField(): Field {
        throw UnsupportedOperationException()
    }

    override fun getFieldType(): GraphQLOutputType {
        return field.getFieldDefinitions(schema).single().type
    }

    override fun getExecutionStepInfo(): ExecutionStepInfo {
        throw UnsupportedOperationException()
    }

    override fun getFragmentsByName(): MutableMap<String, FragmentDefinition> {
        throw UnsupportedOperationException()
    }

    override fun getExecutionId(): ExecutionId {
        throw UnsupportedOperationException()
    }

    override fun getSelectionSet(): DataFetchingFieldSelectionSet {
        throw UnsupportedOperationException()
    }

    override fun getQueryDirectives(): QueryDirectives {
        throw UnsupportedOperationException()
    }

    override fun <K : Any?, V : Any?> getDataLoader(dataLoaderName: String): DataLoader<K, V> {
        throw UnsupportedOperationException()
    }

    override fun getDataLoaderRegistry(): DataLoaderRegistry {
        throw UnsupportedOperationException()
    }

    override fun getCacheControl(): CacheControl {
        throw UnsupportedOperationException()
    }

    override fun getLocale(): Locale {
        return Locale.getDefault()
    }

    override fun getOperationDefinition(): OperationDefinition {
        throw UnsupportedOperationException()
    }

    override fun getDocument(): Document {
        throw UnsupportedOperationException()
    }

    override fun getVariables(): MutableMap<String, Any> {
        throw UnsupportedOperationException()
    }
}
