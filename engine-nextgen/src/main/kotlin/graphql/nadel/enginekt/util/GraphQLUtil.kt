package graphql.nadel.enginekt.util

import graphql.ErrorType
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.ExecutionResultImpl
import graphql.ExecutionResultImpl.newExecutionResult
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder.newError
import graphql.GraphqlErrorException
import graphql.execution.ExecutionId
import graphql.execution.ExecutionIdProvider
import graphql.execution.instrumentation.InstrumentationContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.language.ArrayValue
import graphql.language.BooleanValue
import graphql.language.Definition
import graphql.language.Document
import graphql.language.EnumTypeExtensionDefinition
import graphql.language.FloatValue
import graphql.language.ImplementingTypeDefinition
import graphql.language.InputObjectTypeExtensionDefinition
import graphql.language.IntValue
import graphql.language.InterfaceTypeExtensionDefinition
import graphql.language.NamedNode
import graphql.language.Node
import graphql.language.NullValue
import graphql.language.ObjectField
import graphql.language.ObjectTypeDefinition
import graphql.language.ObjectTypeExtensionDefinition
import graphql.language.ObjectValue
import graphql.language.OperationDefinition
import graphql.language.SDLDefinition
import graphql.language.ScalarTypeExtensionDefinition
import graphql.language.SchemaExtensionDefinition
import graphql.language.StringValue
import graphql.language.Type
import graphql.language.TypeName
import graphql.language.UnionTypeExtensionDefinition
import graphql.language.Value
import graphql.nadel.DefinitionRegistry
import graphql.nadel.OperationKind
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.dsl.UnderlyingServiceHydration
import graphql.nadel.enginekt.transform.query.NadelQueryPath
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.instrumentation.parameters.NadelInstrumentationExecuteOperationParameters
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.ExecutableNormalizedOperation
import graphql.normalized.NormalizedInputValue
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLType
import graphql.schema.GraphQLTypeUtil
import graphql.schema.GraphQLUnionType
import graphql.schema.GraphQLUnmodifiedType
import graphql.schema.idl.TypeUtil
import kotlinx.coroutines.future.asDeferred

internal typealias AnyAstValue = Value<*>
internal typealias AnyAstNode = Node<*>
internal typealias AnyAstDefinition = Definition<*>
internal typealias AnyImplementingTypeDefinition = ImplementingTypeDefinition<*>
internal typealias AnyNamedNode = NamedNode<*>
internal typealias AnySDLDefinition = SDLDefinition<*>
internal typealias AnyAstType = Type<*>

fun newGraphQLError(
    message: String,
    errorType: ErrorType,
    extensions: MutableJsonMap = mutableMapOf(),
): GraphQLError {
    return newError()
        .message(message)
        .errorType(errorType)
        .extensions(extensions)
        .build()
}

fun toGraphQLError(
    raw: JsonMap,
): GraphQLError {
    val errorBuilder = newError()
        .message(raw["message"] as String?)
    raw["extensions"]?.let { extensions ->
        errorBuilder.extensions(extensions as JsonMap)
    }
    raw["path"]?.let { path ->
        errorBuilder.path(path as AnyList)
    }
    return errorBuilder.build()
}

fun GraphQLSchema.getField(coordinates: FieldCoordinates): GraphQLFieldDefinition? {
    return getType(coordinates.typeName)
        ?.let { it as? GraphQLFieldsContainer }
        ?.getField(coordinates.fieldName)
}

fun GraphQLSchema.getOperationType(kind: OperationKind): GraphQLObjectType? {
    return when (kind) {
        OperationKind.QUERY -> queryType
        OperationKind.MUTATION -> mutationType
        OperationKind.SUBSCRIPTION -> subscriptionType
    }
}

fun DefinitionRegistry.getOperationTypes(kind: OperationKind): List<ObjectTypeDefinition> {
    return when (kind) {
        OperationKind.QUERY -> queryType
        OperationKind.MUTATION -> mutationType
        OperationKind.SUBSCRIPTION -> subscriptionType
    }
}

fun GraphQLFieldsContainer.getFieldAt(
    pathToField: List<String>,
): GraphQLFieldDefinition? {
    return getFieldAt(pathToField, pathIndex = 0)
}

fun GraphQLFieldsContainer.getFieldsAlong(
    pathToField: List<String>,
): List<GraphQLFieldDefinition> {
    var parent = this
    return pathToField.mapIndexed { index, fieldName ->
        val field = parent.getField(fieldName)
        if (index != pathToField.lastIndex) {
            parent = field.type.unwrapAll() as GraphQLFieldsContainer
        }
        field
    }
}

private fun GraphQLFieldsContainer.getFieldAt(
    pathToField: List<String>,
    pathIndex: Int,
): GraphQLFieldDefinition? {
    val field = getField(pathToField[pathIndex])

    return if (pathIndex == pathToField.lastIndex) {
        field
    } else {
        val fieldOutputType = field.type.unwrapAll() as GraphQLFieldsContainer
        fieldOutputType.getFieldAt(pathToField, pathIndex + 1)
    }
}

fun ExecutableNormalizedField.toBuilder(): ExecutableNormalizedField.Builder {
    var builder: ExecutableNormalizedField.Builder? = null
    transform { builder = it }
    return builder!!
}

fun ExecutableNormalizedField.copyWithChildren(children: List<ExecutableNormalizedField>): ExecutableNormalizedField {
    fun fixParents(old: ExecutableNormalizedField?, new: ExecutableNormalizedField?) {
        if (old == null || new == null || new.parent == null) {
            return
        }
        val newParent = new.parent.toBuilder()
            .children(old.parent.children.filter { it !== old } + new)
            .build()
        new.replaceParent(newParent)
        // Do recursively for all ancestors
        fixParents(old = old.parent, new = newParent)
    }

    children.forEach {
        it.replaceParent(this)
    }

    return toBuilder()
        .children(children)
        .build()
        .also {
            fixParents(old = this, new = it)
        }
}

val ExecutableNormalizedField.queryPath: NadelQueryPath get() = NadelQueryPath(listOfResultKeys)

inline fun <reified T : AnyAstDefinition> Document.getDefinitionsOfType(): List<T> {
    return getDefinitionsOfType(T::class.java)
}

fun deepClone(fields: List<ExecutableNormalizedField>): List<ExecutableNormalizedField> {
    return fields.map {
        it.toBuilder()
            .children(deepClone(fields = it.children))
            .build()
    }
}

fun GraphQLType.unwrapOne(): GraphQLType {
    return GraphQLTypeUtil.unwrapOne(this)
}

fun GraphQLType.unwrapAll(): GraphQLUnmodifiedType {
    return GraphQLTypeUtil.unwrapAll(this)
}

fun GraphQLType.unwrapNonNull(): GraphQLType {
    return GraphQLTypeUtil.unwrapNonNull(this)
}

val GraphQLType.isList: Boolean get() = GraphQLTypeUtil.isList(this)
val GraphQLType.isNonNull: Boolean get() = GraphQLTypeUtil.isNonNull(this)
val GraphQLType.isWrapped: Boolean get() = GraphQLTypeUtil.isWrapped(this)
val GraphQLType.isNotWrapped: Boolean get() = GraphQLTypeUtil.isNotWrapped(this)

fun AnyAstType.unwrapOne(): AnyAstType {
    return TypeUtil.unwrapOne(this)
}

fun AnyAstType.unwrapAll(): TypeName {
    return TypeUtil.unwrapAll(this)
}

fun AnyAstType.unwrapNonNull(): AnyAstType {
    return if (isNonNull) unwrapOne() else this
}

val AnyAstType.isList: Boolean get() = TypeUtil.isList(this)
val AnyAstType.isNonNull: Boolean get() = TypeUtil.isNonNull(this)
val AnyAstType.isWrapped: Boolean get() = TypeUtil.isWrapped(this)
val AnyAstType.isNotWrapped: Boolean get() = !isWrapped

internal fun mergeResults(results: List<ExecutionResult>): ExecutionResult {
    val data: MutableJsonMap = mutableMapOf()
    val extensions: MutableJsonMap = mutableMapOf()
    val errors: MutableList<GraphQLError> = mutableListOf()

    fun putAndMergeTopLevelData(oneData: JsonMap) {
        for ((topLevelFieldName: String, newTopLevelFieldValue: Any?) in oneData) {
            if (topLevelFieldName in data) {
                val existingValue = data[topLevelFieldName]
                if (existingValue == null) {
                    data[topLevelFieldName] = newTopLevelFieldValue
                } else if (existingValue is AnyMap && newTopLevelFieldValue is AnyMap) {
                    existingValue.asMutableJsonMap().putAll(
                        newTopLevelFieldValue.asJsonMap(),
                    )
                }
            } else {
                data[topLevelFieldName] = newTopLevelFieldValue
            }
        }
    }

    for (result in results) {
        val resultData = result.getData<JsonMap?>()
        if (resultData != null) {
            putAndMergeTopLevelData(resultData)
        }
        errors.addAll(result.errors)
        result.extensions?.asJsonMap()?.let(extensions::putAll)
    }

    return newExecutionResult()
        .data(data)
        .extensions(extensions.let {
            @Suppress("UNCHECKED_CAST") // .extensions should take in a Map<*, *> instead of strictly Map<Any?, Any?>
            it as Map<Any?, Any?>
        }.takeIf {
            it.isNotEmpty()
        })
        .errors(errors)
        .build()
}

fun makeFieldCoordinates(typeName: String, fieldName: String): FieldCoordinates {
    return FieldCoordinates.coordinates(typeName, fieldName)
}

fun makeFieldCoordinates(parentType: GraphQLObjectType, field: GraphQLFieldDefinition): FieldCoordinates {
    return makeFieldCoordinates(typeName = parentType.name, fieldName = field.name)
}

fun ExecutionIdProvider.provide(executionInput: ExecutionInput): ExecutionId? {
    return provide(executionInput.query, executionInput.operationName, executionInput.context)
}

fun ServiceExecutionResult.copy(
    data: MutableJsonMap = this.data,
    errors: MutableList<MutableJsonMap> = this.errors,
    extensions: MutableJsonMap = this.extensions,
): ServiceExecutionResult {
    return newServiceExecutionResult(data, errors, extensions)
}

fun newServiceExecutionResult(
    data: MutableJsonMap = mutableMapOf(),
    errors: MutableList<MutableJsonMap> = mutableListOf(),
    extensions: MutableJsonMap = mutableMapOf(),
): ServiceExecutionResult {
    return ServiceExecutionResult(data, errors, extensions)
}

fun newServiceExecutionResult(
    error: GraphQLError,
): ServiceExecutionResult {
    return newServiceExecutionResult(
        errors = mutableListOf(
            error.toSpecification(),
        ),
    )
}

fun newExecutionResult(
    error: GraphQLError,
    data: Any? = null,
): ExecutionResultImpl {
    return newExecutionResult()
        .data(data)
        .addError(error)
        .build()
}

fun ExecutableNormalizedField.getOperationKind(
    schema: GraphQLSchema,
): OperationDefinition.Operation {
    val objectTypeName = objectTypeNames.singleOrNull()
        ?: error("Top level field can only belong to one operation type")
    return when {
        schema.queryType.name == objectTypeName -> OperationDefinition.Operation.QUERY
        schema.mutationType?.name?.equals(objectTypeName) == true -> OperationDefinition.Operation.MUTATION
        schema.subscriptionType?.name?.equals(objectTypeName) == true -> OperationDefinition.Operation.SUBSCRIPTION
        else -> error("Type '$objectTypeName' is not one of the standard GraphQL operation types")
    }
}

/**
 * Standard [Document.getOperationDefinition] does not suit needs as it doesn't handle null
 * operation names, and returns a yucky [java.util.Optional].
 *
 * Don't use [graphql.language.NodeUtil.getOperation] because that does weird things and stores
 * operation and fragment definitions in [Map]s.
 */
internal fun Document.getOperationDefinitionOrNull(operationName: String?): OperationDefinition? {
    if (operationName == null || operationName.isEmpty()) {
        return definitions.singleOfTypeOrNull()
    }

    return definitions.singleOfTypeOrNull<OperationDefinition> { def ->
        def.name == operationName
    }
}

internal suspend fun NadelInstrumentation.beginExecute(
    query: ExecutableNormalizedOperation,
    queryDocument: Document,
    executionInput: ExecutionInput,
    graphQLSchema: GraphQLSchema,
    instrumentationState: InstrumentationState?,
): InstrumentationContext<ExecutionResult>? {
    val nadelInstrumentationExecuteOperationParameters = NadelInstrumentationExecuteOperationParameters(
        query,
        queryDocument,
        graphQLSchema,
        executionInput.variables,
        queryDocument.getOperationDefinitionOrNull(executionInput.operationName)
            ?: error("Unable to find operation. This should not happen. Query document should be valid by now."),
        instrumentationState,
        executionInput.context,
    )

    return beginExecute(nadelInstrumentationExecuteOperationParameters)
        .asDeferred()
        .await()
}

/**
 * Turns GraphQL types to object types when possible e.g. finds concrete implementations
 * for interfaces, gets object types inside unions, and returns objects as is.
 */
fun resolveObjectTypes(
    schema: GraphQLSchema,
    type: GraphQLType,
    onNotObjectType: (GraphQLType) -> Nothing,
): List<GraphQLObjectType> {
    return when (val unwrappedType = type.unwrapAll()) {
        is GraphQLObjectType -> listOf(unwrappedType)
        is GraphQLUnionType -> unwrappedType.types.flatMap {
            resolveObjectTypes(schema, type = it, onNotObjectType)
        }
        is GraphQLInterfaceType -> schema.getImplementations(unwrappedType)
        else -> onNotObjectType(unwrappedType)
    }
}

/**
 * Creates a GraphQLErrorException based on the data of this GraphQLError
 */
fun GraphQLError.toGraphQLErrorException(): GraphqlErrorException {
    return GraphqlErrorException.newErrorException()
        .message(this.message)
        .sourceLocations(this.locations)
        .errorClassification(this.errorType)
        .path(this.path)
        .extensions(this.extensions)
        .build()
}

val AnyAstNode.isExtensionDef: Boolean
    get() {
        return this is ObjectTypeExtensionDefinition
            || this is InterfaceTypeExtensionDefinition
            || this is EnumTypeExtensionDefinition
            || this is ScalarTypeExtensionDefinition
            || this is InputObjectTypeExtensionDefinition
            || this is SchemaExtensionDefinition
            || this is UnionTypeExtensionDefinition
    }

fun makeNormalizedInputValue(
    type: GraphQLInputType,
    value: AnyAstValue,
): NormalizedInputValue {
    return NormalizedInputValue(
        GraphQLTypeUtil.simplePrint(type), // type name
        value, // value
    )
}

internal fun javaValueToAstValue(value: Any?): AnyAstValue {
    return when (value) {
        is AnyList -> ArrayValue(
            value.map(::javaValueToAstValue),
        )
        is AnyMap -> ObjectValue
            .newObjectValue()
            .objectFields(
                value.asJsonMap().map {
                    ObjectField(it.key, javaValueToAstValue(it.value))
                },
            )
            .build()
        null -> NullValue
            .newNullValue()
            .build()
        is Double -> FloatValue.newFloatValue()
            .value(value.toBigDecimal())
            .build()
        is Float -> FloatValue.newFloatValue()
            .value(value.toBigDecimal())
            .build()
        is Number -> IntValue.newIntValue()
            .value(value.toLong().toBigInteger())
            .build()
        is String -> StringValue.newStringValue()
            .value(value)
            .build()
        is Boolean -> BooleanValue.newBooleanValue()
            .value(value)
            .build()
        else -> error("Unknown value type '${value.javaClass.name}'")
    }
}

val UnderlyingServiceHydration.pathToActorField
    get() = listOfNotNull(syntheticField, topLevelField)

val GraphQLSchema.operationTypes
    get() = listOfNotNull(queryType, mutationType, subscriptionType)
