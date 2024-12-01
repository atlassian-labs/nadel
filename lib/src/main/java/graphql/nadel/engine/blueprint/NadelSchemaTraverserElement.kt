package graphql.nadel.engine.blueprint

import graphql.nadel.engine.util.unwrapAll
import graphql.schema.GraphQLAppliedDirective
import graphql.schema.GraphQLAppliedDirectiveArgument
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLDirective
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLEnumValueDefinition
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLInputFieldsContainer
import graphql.schema.GraphQLInputObjectField
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLNamedSchemaElement
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLUnionType

internal sealed interface NadelSchemaTraverserElement {
    val node: GraphQLNamedSchemaElement

    fun forEachChild(onElement: (NadelSchemaTraverserElement) -> Unit)

    sealed interface Type : NadelSchemaTraverserElement {
        override val node: GraphQLNamedType
    }

    sealed interface Argument : NadelSchemaTraverserElement {
        val parent: GraphQLNamedSchemaElement
        override val node: GraphQLArgument

        override fun forEachChild(onElement: (NadelSchemaTraverserElement) -> Unit) {
            onElement(InputType.from(node.type))
        }
    }

    sealed interface OutputType : NadelSchemaTraverserElement, Type {
        companion object {
            fun from(type: GraphQLOutputType): OutputType {
                return when (val node = type.unwrapAll()) {
                    is GraphQLEnumType -> EnumType(node)
                    is GraphQLInterfaceType -> InterfaceType(node)
                    is GraphQLObjectType -> ObjectType(node)
                    is GraphQLScalarType -> ScalarType(node)
                    is GraphQLUnionType -> UnionType(node)
                    else -> throw UnsupportedOperationException(type.javaClass.name)
                }
            }
        }
    }

    sealed interface InputType : NadelSchemaTraverserElement, Type {
        companion object {
            fun from(type: GraphQLInputType): InputType {
                return when (val node = type.unwrapAll()) {
                    is GraphQLEnumType -> EnumType(node)
                    is GraphQLInputObjectType -> InputObjectType(node)
                    is GraphQLScalarType -> ScalarType(node)
                    else -> throw UnsupportedOperationException(type.javaClass.name)
                }
            }
        }
    }

    data class FieldArgument(
        override val parent: GraphQLFieldDefinition,
        override val node: GraphQLArgument,
    ) : Argument

    data class DirectiveArgument(
        override val parent: GraphQLDirective,
        override val node: GraphQLArgument,
    ) : Argument

    data class AppliedDirectiveArgument(
        val parent: GraphQLAppliedDirective,
        override val node: GraphQLAppliedDirectiveArgument,
    ) : NadelSchemaTraverserElement {
        override fun forEachChild(onElement: (NadelSchemaTraverserElement) -> Unit) {
            onElement(InputType.from(node.type))
        }
    }

    data class UnionType(
        override val node: GraphQLUnionType,
    ) : NadelSchemaTraverserElement, Type, OutputType {
        override fun forEachChild(onElement: (NadelSchemaTraverserElement) -> Unit) {
            node.types.forEach { member ->
                onElement(UnionMemberType(node, member as GraphQLObjectType))
            }
        }
    }

    /**
     * Note: this is an intermediary element used to determine whether a union member should
     * be traversed. The actual member [UnionMemberType.node] will be traversed if so.
     *
     * As such, it does not extend [Type] as this is only an intermediary.
     */
    data class UnionMemberType(
        val parent: GraphQLUnionType,
        override val node: GraphQLObjectType,
    ) : NadelSchemaTraverserElement {
        override fun forEachChild(onElement: (NadelSchemaTraverserElement) -> Unit) {
            onElement(ObjectType(node))
        }
    }

    data class InterfaceType(
        override val node: GraphQLInterfaceType,
    ) : NadelSchemaTraverserElement, Type, OutputType {
        override fun forEachChild(onElement: (NadelSchemaTraverserElement) -> Unit) {
            node.fields.forEach { field ->
                onElement(FieldDefinition(node, field))
            }
            node.interfaces.forEach { parentType ->
                onElement(InterfaceType(parentType as GraphQLInterfaceType))
            }
        }
    }

    data class EnumType(
        override val node: GraphQLEnumType,
    ) : NadelSchemaTraverserElement, Type, InputType, OutputType {
        override fun forEachChild(onElement: (NadelSchemaTraverserElement) -> Unit) {
            node.values.forEach { value ->
                onElement(EnumValueDefinition(node, value))
            }
        }
    }

    data class EnumValueDefinition(
        val parent: GraphQLEnumType,
        override val node: GraphQLEnumValueDefinition,
    ) : NadelSchemaTraverserElement {
        override fun forEachChild(onElement: (NadelSchemaTraverserElement) -> Unit) {
        }
    }

    data class FieldDefinition(
        val parent: GraphQLFieldsContainer,
        override val node: GraphQLFieldDefinition,
    ) : NadelSchemaTraverserElement {
        override fun forEachChild(onElement: (NadelSchemaTraverserElement) -> Unit) {
            onElement(OutputType.from(node.type))
            node.arguments.forEach { arg ->
                onElement(FieldArgument(node, arg))
            }
        }
    }

    data class InputObjectField(
        val parent: GraphQLInputFieldsContainer,
        override val node: GraphQLInputObjectField,
    ) : NadelSchemaTraverserElement {
        override fun forEachChild(onElement: (NadelSchemaTraverserElement) -> Unit) {
            onElement(InputType.from(node.type))
        }
    }

    data class InputObjectType(
        override val node: GraphQLInputObjectType,
    ) : NadelSchemaTraverserElement, Type, InputType {
        override fun forEachChild(onElement: (NadelSchemaTraverserElement) -> Unit) {
            node.fields.forEach { field ->
                onElement(InputObjectField(node, field))
            }
        }
    }

    data class ObjectType(
        override val node: GraphQLObjectType,
    ) : NadelSchemaTraverserElement, Type, OutputType {
        override fun forEachChild(onElement: (NadelSchemaTraverserElement) -> Unit) {
            node.fields.forEach { field ->
                onElement(FieldDefinition(node, field))
            }
            node.interfaces.forEach { parentType ->
                onElement(InterfaceType(parentType as GraphQLInterfaceType))
            }
        }
    }

    data class ScalarType(
        override val node: GraphQLScalarType,
    ) : NadelSchemaTraverserElement, Type, InputType, OutputType {
        override fun forEachChild(onElement: (NadelSchemaTraverserElement) -> Unit) {
        }
    }

    data class Directive(
        override val node: GraphQLDirective,
    ) : NadelSchemaTraverserElement {
        override fun forEachChild(onElement: (NadelSchemaTraverserElement) -> Unit) {
            node.arguments.forEach { arg ->
                onElement(DirectiveArgument(node, arg))
            }
        }
    }

    data class AppliedDirective(
        val parent: GraphQLNamedSchemaElement,
        override val node: GraphQLAppliedDirective,
    ) : NadelSchemaTraverserElement {
        override fun forEachChild(onElement: (NadelSchemaTraverserElement) -> Unit) {
            node.arguments.forEach { arg ->
                onElement(AppliedDirectiveArgument(node, arg))
            }
        }
    }

    companion object {
        fun from(type: GraphQLNamedSchemaElement): NadelSchemaTraverserElement {
            return when (type) {
                is GraphQLEnumType -> {
                    EnumType(type)
                }
                is GraphQLInputObjectType -> {
                    InputObjectType(type)
                }
                is GraphQLInterfaceType -> {
                    InterfaceType(type)
                }
                is GraphQLObjectType -> {
                    ObjectType(type)
                }
                is GraphQLScalarType -> {
                    ScalarType(type)
                }
                is GraphQLUnionType -> {
                    UnionType(type)
                }
                is GraphQLDirective -> {
                    Directive(type)
                }
                else -> {
                    throw UnsupportedOperationException(type.javaClass.name)
                }
            }
        }
    }
}
