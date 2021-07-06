package graphql.nadel.enginekt.util

import graphql.ErrorType
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.ExecutionResultImpl.newExecutionResult
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder.newError
import graphql.execution.ExecutionId
import graphql.execution.ExecutionIdProvider
import graphql.language.Definition
import graphql.language.Document
import graphql.language.ObjectTypeDefinition
import graphql.nadel.DefinitionRegistry
import graphql.nadel.OperationKind
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.transform.query.NadelQueryPath
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLType
import graphql.schema.GraphQLTypeUtil

typealias AnyAstDefinition = Definition<*>

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
    return newError()
        .message(raw["message"] as String?)
        .build()
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
            parent = field.type.unwrap(all = true) as GraphQLFieldsContainer
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
        val fieldOutputType = field.type.unwrap(all = true) as GraphQLFieldsContainer
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

fun GraphQLType.unwrap(
    all: Boolean = false,
): GraphQLType {
    return when (all) {
        true -> GraphQLTypeUtil.unwrapAll(this)
        else -> GraphQLTypeUtil.unwrapOne(this)
    }
}

fun GraphQLType.unwrapNonNull(): GraphQLType {
    return GraphQLTypeUtil.unwrapNonNull(this)
}

val GraphQLType.isList: Boolean get() = GraphQLTypeUtil.isList(this)
val GraphQLType.isNonNull: Boolean get() = GraphQLTypeUtil.isNonNull(this)
val GraphQLType.isWrapped: Boolean get() = GraphQLTypeUtil.isWrapped(this)
val GraphQLType.isNotWrapped: Boolean get() = GraphQLTypeUtil.isNotWrapped(this)

internal fun mergeResults(results: List<ExecutionResult>): ExecutionResult {
    val data: MutableJsonMap = mutableMapOf()
    val extensions: MutableJsonMap = mutableMapOf()
    val errors: MutableList<GraphQLError> = mutableListOf()

    for (result in results) {
        val resultData = result.getData<JsonMap?>()
        if (resultData != null) {
            updateOverallResultAndMergeSameNameTopLevelFields(data, resultData)
        }
        errors.addAll(result.errors)
        result.extensions?.asJsonMap()?.let(extensions::putAll)
    }

    return newExecutionResult()
        .data(data.takeIf {
            it.isNotEmpty()
        })
        .extensions(extensions.let {
            @Suppress("UNCHECKED_CAST") // .extensions should take in a Map<*, *> instead of strictly Map<Any?, Any?>
            it as Map<Any?, Any?>
        }.takeIf {
            it.isNotEmpty()
        })
        .errors(errors)
        .build()
}

internal fun updateOverallResultAndMergeSameNameTopLevelFields(
    overallResultMap: MutableJsonMap,
    oneResultMap: JsonMap,
) {
    for ((topLevelFieldName, newTopLevelFieldChildren) in oneResultMap) {
        if (overallResultMap.containsKey(topLevelFieldName)) {
            val existingTopLevelFieldMap = overallResultMap[topLevelFieldName]
            if (newTopLevelFieldChildren is AnyMap && existingTopLevelFieldMap is AnyMutableMap) {
                (existingTopLevelFieldMap.asMutableJsonMap()).putAll(newTopLevelFieldChildren.asJsonMap())
            }
        } else {
            overallResultMap[topLevelFieldName] = newTopLevelFieldChildren
        }
    }
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
