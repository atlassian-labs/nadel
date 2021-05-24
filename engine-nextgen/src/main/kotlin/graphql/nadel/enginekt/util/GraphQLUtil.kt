package graphql.nadel.enginekt.util

import graphql.nadel.OperationKind
import graphql.normalized.NormalizedField
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema

fun GraphQLSchema.getOperationType(kind: OperationKind): GraphQLObjectType? {
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

fun NormalizedField.copy(
        objectType: GraphQLObjectType = this.objectType,
        alias: String? = this.alias,
        arguments: Map<String, Any?> = this.arguments,
        fieldDefinition: GraphQLFieldDefinition = this.fieldDefinition,
        children: List<NormalizedField> = this.children,
        level: Int = this.level,
        parent: NormalizedField? = this.parent,
): NormalizedField {
    return transform { builder ->
        builder.objectType(objectType)
            .alias(alias)
            .arguments(arguments)
            .fieldDefinition(fieldDefinition)
            .children(children)
            .level(level)
            .parent(parent)
    }
}
