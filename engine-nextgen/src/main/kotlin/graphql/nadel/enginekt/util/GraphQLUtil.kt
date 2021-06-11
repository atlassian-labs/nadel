package graphql.nadel.enginekt.util

import graphql.ExecutionResult
import graphql.ExecutionResultImpl.newExecutionResult
import graphql.GraphQLError
import graphql.language.Definition
import graphql.language.Document
import graphql.language.ObjectTypeDefinition
import graphql.nadel.DefinitionRegistry
import graphql.nadel.OperationKind
import graphql.nadel.enginekt.transform.query.QueryPath
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLType
import graphql.schema.GraphQLTypeUtil

typealias AnyAstDefinition = Definition<*>

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

private fun GraphQLFieldsContainer.getFieldAt(
    pathToField: List<String>,
    pathIndex: Int,
): GraphQLFieldDefinition? {
    val field = getField(pathToField[pathIndex])

    return if (pathIndex == pathToField.lastIndex) {
        field
    } else {
        val fieldOutputType = field.type as GraphQLFieldsContainer
        fieldOutputType.getFieldAt(pathToField, pathIndex + 1)
    }
}

fun NormalizedField.toBuilder(): NormalizedField.Builder {
    var builder: NormalizedField.Builder? = null
    transform { builder = it }
    return builder!!
}

fun NormalizedField.copyWithChildren(children: List<NormalizedField>): NormalizedField {
    fun fixParents(old: NormalizedField?, new: NormalizedField?) {
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

val NormalizedField.queryPath: QueryPath get() = QueryPath(listOfResultKeys)

inline fun <reified T : AnyAstDefinition> Document.getDefinitionsOfType(): List<T> {
    return getDefinitionsOfType(T::class.java)
}

fun deepClone(fields: List<NormalizedField>): List<NormalizedField> {
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

internal fun mergeResults(results: List<ExecutionResult>): ExecutionResult {
    val data: MutableJsonMap = mutableMapOf()
    val extensions: MutableJsonMap = mutableMapOf()
    val errors: MutableList<GraphQLError> = mutableListOf()

    for (result in results) {
        if (result.isDataPresent) {
            data.putAll(result.getData())
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
