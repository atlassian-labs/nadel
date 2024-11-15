package graphql.nadel.engine.blueprint

import graphql.nadel.engine.util.unwrapAll
import graphql.schema.GraphQLAppliedDirective
import graphql.schema.GraphQLAppliedDirectiveArgument
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLCompositeType
import graphql.schema.GraphQLDirective
import graphql.schema.GraphQLDirectiveContainer
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLEnumValueDefinition
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLInputFieldsContainer
import graphql.schema.GraphQLInputObjectField
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLList
import graphql.schema.GraphQLModifiedType
import graphql.schema.GraphQLNamedSchemaElement
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLNonNull
import graphql.schema.GraphQLNullableType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLSchemaElement
import graphql.schema.GraphQLType
import graphql.schema.GraphQLTypeReference
import graphql.schema.GraphQLUnionType
import graphql.schema.GraphQLUnmodifiedType

/**
 * Significantly faster than normal [graphql.schema.SchemaTraverser] as it's simpler.
 *
 * Validating central schema with old [graphql.schema.SchemaTraverser] takes 306ms and this impl takes 74ms.
 *
 * That's 4x faster.
 */
internal class NadelFastSchemaTraverser {
    fun traverse(
        schema: GraphQLSchema,
        roots: List<String>,
        visitor: Visitor,
    ) {
        val queue = roots
            .mapTo(mutableListOf()) {
                (null as GraphQLNamedSchemaElement?) to (schema.typeMap[it]!! as GraphQLNamedSchemaElement)
            }
        val visited = roots.toMutableSet()

        fun queueType(parent: GraphQLNamedSchemaElement, type: GraphQLNamedType) {
            if (!visited.contains(type.name)) {
                visited.add(type.name)
                queue.add(parent to type)
            }
        }

        fun queueElement(parent: GraphQLNamedSchemaElement, element: GraphQLNamedSchemaElement) {
            queue.add(parent to element)
        }

        while (queue.isNotEmpty()) {
            val (parent, element) = queue.removeLast()
            val result = when (element) {
                is GraphQLObjectType -> visitor.visitGraphQLObjectType(parent, element)
                is GraphQLFieldDefinition -> visitor.visitGraphQLFieldDefinition(parent, element)
                is GraphQLScalarType -> visitor.visitGraphQLScalarType(parent, element)
                is GraphQLInputObjectType -> visitor.visitGraphQLInputObjectType(parent, element)
                is GraphQLInputObjectField -> visitor.visitGraphQLInputObjectField(parent, element)
                is GraphQLAppliedDirective -> visitor.visitGraphQLAppliedDirective(parent, element)
                is GraphQLDirective -> visitor.visitGraphQLDirective(parent, element)
                is GraphQLArgument -> visitor.visitGraphQLArgument(parent, element)
                is GraphQLEnumType -> visitor.visitGraphQLEnumType(parent, element)
                is GraphQLEnumValueDefinition -> visitor.visitGraphQLEnumValueDefinition(parent, element)
                is GraphQLInterfaceType -> visitor.visitGraphQLInterfaceType(parent, element)
                is GraphQLUnionType -> visitor.visitGraphQLUnionType(parent, element)
                else -> false
            }

            if (!result) {
                continue
            }

            // Handle next
            element.forEachChild { child ->
                when (child) {
                    is GraphQLType -> {
                        queueType(parent = element, child.unwrapAll())
                    }
                    is GraphQLNamedSchemaElement -> {
                        queueElement(parent = element, child)
                    }
                }
            }
        }
    }

    interface Visitor {
        fun visitGraphQLArgument(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLArgument,
        ): Boolean

        fun visitGraphQLUnionType(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLUnionType,
        ): Boolean

        fun visitGraphQLInterfaceType(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLInterfaceType,
        ): Boolean

        fun visitGraphQLEnumType(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLEnumType,
        ): Boolean

        fun visitGraphQLEnumValueDefinition(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLEnumValueDefinition,
        ): Boolean

        fun visitGraphQLFieldDefinition(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLFieldDefinition,
        ): Boolean

        fun visitGraphQLInputObjectField(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLInputObjectField,
        ): Boolean

        fun visitGraphQLInputObjectType(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLInputObjectType,
        ): Boolean

        fun visitGraphQLList(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLList,
        ): Boolean

        fun visitGraphQLNonNull(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLNonNull,
        ): Boolean

        fun visitGraphQLObjectType(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLObjectType,
        ): Boolean

        fun visitGraphQLScalarType(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLScalarType,
        ): Boolean

        fun visitGraphQLTypeReference(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLTypeReference,
        ): Boolean

        fun visitGraphQLModifiedType(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLModifiedType,
        ): Boolean

        fun visitGraphQLCompositeType(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLCompositeType,
        ): Boolean

        fun visitGraphQLDirective(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLDirective,
        ): Boolean

        fun visitGraphQLDirectiveContainer(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLDirectiveContainer,
        ): Boolean

        fun visitGraphQLFieldsContainer(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLFieldsContainer,
        ): Boolean

        fun visitGraphQLInputFieldsContainer(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLInputFieldsContainer,
        ): Boolean

        fun visitGraphQLNullableType(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLNullableType,
        ): Boolean

        fun visitGraphQLOutputType(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLOutputType,
        ): Boolean

        fun visitGraphQLUnmodifiedType(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLUnmodifiedType,
        ): Boolean

        fun visitGraphQLAppliedDirective(
            parent: GraphQLNamedSchemaElement?,
            node: GraphQLAppliedDirective,
        ): Boolean
    }
}

private inline fun GraphQLSchemaElement.forEachChild(
    onElement: (GraphQLSchemaElement) -> Unit,
) {
    when (this) {
        is GraphQLAppliedDirective -> {
            arguments.forEach(onElement)
        }
        is GraphQLAppliedDirectiveArgument -> {
            onElement(type)
        }
        is GraphQLArgument -> {
            onElement(type)
        }
        is GraphQLDirective -> {
            arguments.forEach(onElement)
        }
        is GraphQLEnumType -> {
            values.forEach(onElement)
        }
        is GraphQLEnumValueDefinition -> {
        }
        is GraphQLFieldDefinition -> {
            onElement(type)
            arguments.forEach(onElement)
        }
        is GraphQLInputObjectField -> {
            onElement(type)
        }
        is GraphQLInputObjectType -> {
            fields.forEach(onElement)
        }
        is GraphQLInterfaceType -> {
            fields.forEach(onElement)
            interfaces.forEach(onElement)
        }
        is GraphQLList -> {
            onElement(unwrapAll())
        }
        is GraphQLNonNull -> {
            onElement(unwrapAll())
        }
        is GraphQLObjectType -> {
            fields.forEach(onElement)
            interfaces.forEach(onElement)
        }
        is GraphQLScalarType -> {
        }
        is GraphQLUnionType -> {
            types.forEach(onElement)
        }
        else -> {
            throw UnsupportedOperationException()
        }
    }
}
