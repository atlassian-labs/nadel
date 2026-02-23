package graphql.nadel.engine.blueprint

import graphql.nadel.definition.coordinates.NadelAppliedDirectiveArgumentCoordinates
import graphql.nadel.definition.coordinates.NadelAppliedDirectiveCoordinates
import graphql.nadel.definition.coordinates.NadelAppliedDirectiveParentCoordinates
import graphql.nadel.definition.coordinates.NadelArgumentCoordinates
import graphql.nadel.definition.coordinates.NadelArgumentParentCoordinates
import graphql.nadel.definition.coordinates.NadelDirectiveCoordinates
import graphql.nadel.definition.coordinates.NadelEnumCoordinates
import graphql.nadel.definition.coordinates.NadelEnumValueCoordinates
import graphql.nadel.definition.coordinates.NadelFieldContainerCoordinates
import graphql.nadel.definition.coordinates.NadelFieldCoordinates
import graphql.nadel.definition.coordinates.NadelInputObjectCoordinates
import graphql.nadel.definition.coordinates.NadelInputObjectFieldCoordinates
import graphql.nadel.definition.coordinates.NadelInterfaceCoordinates
import graphql.nadel.definition.coordinates.NadelObjectCoordinates
import graphql.nadel.definition.coordinates.NadelScalarCoordinates
import graphql.nadel.definition.coordinates.NadelSchemaMemberCoordinates
import graphql.nadel.definition.coordinates.NadelTypeCoordinates
import graphql.nadel.definition.coordinates.NadelUnionCoordinates
import graphql.nadel.definition.coordinates.coordinates
import graphql.nadel.engine.util.whenType
import graphql.nadel.engine.util.whenUnmodifiedType
import graphql.schema.GraphQLAppliedDirective
import graphql.schema.GraphQLAppliedDirectiveArgument
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLDirective
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLEnumValueDefinition
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLInputObjectField
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLNamedInputType
import graphql.schema.GraphQLNamedOutputType
import graphql.schema.GraphQLNamedSchemaElement
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLUnionType

internal sealed interface NadelSchemaTraverserElement {
    val parent: NadelSchemaTraverserElement?

    val node: GraphQLNamedSchemaElement

    fun coordinates(): NadelSchemaMemberCoordinates

    fun forEachChild(onElement: (NadelSchemaTraverserElement) -> Unit)

    sealed interface Type : NadelSchemaTraverserElement {
        override val node: GraphQLNamedType

        override fun coordinates(): NadelTypeCoordinates
    }

    sealed interface FieldsContainer : NadelSchemaTraverserElement {
        override val node: GraphQLFieldsContainer

        override fun coordinates(): NadelFieldContainerCoordinates
    }

    sealed interface AppliedDirectiveParent : NadelSchemaTraverserElement {
        override fun coordinates(): NadelAppliedDirectiveParentCoordinates
    }

    sealed interface ArgumentParent : NadelSchemaTraverserElement {
        override fun coordinates(): NadelArgumentParentCoordinates
    }

    sealed interface Argument : NadelSchemaTraverserElement {
        override val parent: ArgumentParent

        override val node: GraphQLArgument

        override fun coordinates(): NadelArgumentCoordinates {
            return parent.coordinates().argument(node.name)
        }

        override fun forEachChild(onElement: (NadelSchemaTraverserElement) -> Unit) {
            onElement(InputType.from(node.type))
        }
    }

    sealed interface OutputType : NadelSchemaTraverserElement, Type {
        override val node: GraphQLNamedOutputType

        override fun coordinates(): NadelTypeCoordinates

        companion object {
            fun from(type: GraphQLOutputType): OutputType {
                return type.whenUnmodifiedType(
                    enumType = ::EnumType,
                    interfaceType = ::InterfaceType,
                    objectType = ::ObjectType,
                    scalarType = ::ScalarType,
                    unionType = ::UnionType,
                )
            }
        }
    }

    sealed interface InputType : NadelSchemaTraverserElement, Type {
        override val node: GraphQLNamedInputType

        override fun coordinates(): NadelTypeCoordinates

        companion object {
            fun from(type: GraphQLInputType): InputType {
                return type.whenUnmodifiedType(
                    enumType = ::EnumType,
                    inputObjectType = ::InputObjectType,
                    scalarType = ::ScalarType,
                )
            }
        }
    }

    data class FieldArgument(
        override val parent: FieldDefinition,
        override val node: GraphQLArgument,
    ) : Argument

    data class DirectiveArgument(
        override val parent: Directive,
        override val node: GraphQLArgument,
    ) : Argument

    data class AppliedDirectiveArgument(
        override val parent: AppliedDirective,
        override val node: GraphQLAppliedDirectiveArgument,
    ) : NadelSchemaTraverserElement {
        override fun coordinates(): NadelAppliedDirectiveArgumentCoordinates {
            return parent.coordinates().argument(node.name)
        }

        override fun forEachChild(onElement: (NadelSchemaTraverserElement) -> Unit) {
            onElement(InputType.from(node.type))
        }
    }

    data class UnionType(
        override val node: GraphQLUnionType,
    ) : NadelSchemaTraverserElement, Type, OutputType {
        override val parent: NadelSchemaTraverserElement? = null

        override fun coordinates(): NadelUnionCoordinates {
            return node.coordinates()
        }

        override fun forEachChild(onElement: (NadelSchemaTraverserElement) -> Unit) {
            node.types.forEach { member ->
                onElement(UnionMemberType(this, member as GraphQLObjectType))
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
        override val parent: UnionType,
        override val node: GraphQLObjectType,
    ) : NadelSchemaTraverserElement {
        override fun coordinates(): NadelObjectCoordinates {
            return node.coordinates()
        }

        override fun forEachChild(onElement: (NadelSchemaTraverserElement) -> Unit) {
            onElement(ObjectType(node))
        }
    }

    data class InterfaceType(
        override val node: GraphQLInterfaceType,
    ) : NadelSchemaTraverserElement, Type, OutputType, FieldsContainer {
        override val parent: NadelSchemaTraverserElement? = null

        override fun coordinates(): NadelInterfaceCoordinates {
            return node.coordinates()
        }

        override fun forEachChild(onElement: (NadelSchemaTraverserElement) -> Unit) {
            node.fields.forEach { field ->
                onElement(FieldDefinition(this, field))
            }
            node.interfaces.forEach { parentType ->
                onElement(InterfaceType(parentType as GraphQLInterfaceType))
            }
        }
    }

    data class EnumType(
        override val node: GraphQLEnumType,
    ) : NadelSchemaTraverserElement, Type, InputType, OutputType {
        override val parent: NadelSchemaTraverserElement? = null

        override fun coordinates(): NadelEnumCoordinates {
            return node.coordinates()
        }

        override fun forEachChild(onElement: (NadelSchemaTraverserElement) -> Unit) {
            node.values.forEach { value ->
                onElement(EnumValueDefinition(this, value))
            }
        }
    }

    data class EnumValueDefinition(
        override val parent: EnumType,
        override val node: GraphQLEnumValueDefinition,
    ) : NadelSchemaTraverserElement {
        override fun coordinates(): NadelEnumValueCoordinates {
            return parent.coordinates().enumValue(node.name)
        }

        override fun forEachChild(onElement: (NadelSchemaTraverserElement) -> Unit) {
        }
    }

    data class FieldDefinition(
        override val parent: FieldsContainer,
        override val node: GraphQLFieldDefinition,
    ) : NadelSchemaTraverserElement, ArgumentParent {
        override fun coordinates(): NadelFieldCoordinates {
            return parent.coordinates().field(node.name)
        }

        override fun forEachChild(onElement: (NadelSchemaTraverserElement) -> Unit) {
            onElement(OutputType.from(node.type))

            node.arguments.forEach { arg ->
                onElement(FieldArgument(this, arg))
            }
        }
    }

    data class InputObjectField(
        override val parent: InputObjectType,
        override val node: GraphQLInputObjectField,
    ) : NadelSchemaTraverserElement {
        override fun coordinates(): NadelInputObjectFieldCoordinates {
            return parent.coordinates().field(node.name)
        }

        override fun forEachChild(onElement: (NadelSchemaTraverserElement) -> Unit) {
            onElement(InputType.from(node.type))
        }
    }

    data class InputObjectType(
        override val node: GraphQLInputObjectType,
    ) : NadelSchemaTraverserElement, Type, InputType {
        override val parent: NadelSchemaTraverserElement? = null

        override fun coordinates(): NadelInputObjectCoordinates {
            return node.coordinates()
        }

        override fun forEachChild(onElement: (NadelSchemaTraverserElement) -> Unit) {
            node.fields.forEach { field ->
                onElement(InputObjectField(this, field))
            }
        }
    }

    data class ObjectType(
        override val node: GraphQLObjectType,
    ) : NadelSchemaTraverserElement, Type, OutputType, FieldsContainer {
        override val parent: NadelSchemaTraverserElement? = null

        override fun coordinates(): NadelObjectCoordinates {
            return node.coordinates()
        }

        override fun forEachChild(onElement: (NadelSchemaTraverserElement) -> Unit) {
            node.fields.forEach { field ->
                onElement(FieldDefinition(this, field))
            }

            node.interfaces.forEach { parentType ->
                onElement(InterfaceType(parentType as GraphQLInterfaceType))
            }
        }
    }

    data class ScalarType(
        override val node: GraphQLScalarType,
    ) : NadelSchemaTraverserElement, Type, InputType, OutputType {
        override val parent: NadelSchemaTraverserElement? = null

        override fun coordinates(): NadelScalarCoordinates {
            return node.coordinates()
        }

        override fun forEachChild(onElement: (NadelSchemaTraverserElement) -> Unit) {
        }
    }

    data class Directive(
        override val node: GraphQLDirective,
    ) : NadelSchemaTraverserElement, ArgumentParent {
        override val parent: NadelSchemaTraverserElement? = null

        override fun coordinates(): NadelDirectiveCoordinates {
            return node.coordinates()
        }

        override fun forEachChild(onElement: (NadelSchemaTraverserElement) -> Unit) {
            node.arguments.forEach { arg ->
                onElement(DirectiveArgument(this, arg))
            }
        }
    }

    data class AppliedDirective(
        override val parent: AppliedDirectiveParent,
        override val node: GraphQLAppliedDirective,
    ) : NadelSchemaTraverserElement {
        override fun forEachChild(onElement: (NadelSchemaTraverserElement) -> Unit) {
            node.arguments.forEach { arg ->
                onElement(AppliedDirectiveArgument(this, arg))
            }
        }

        override fun coordinates(): NadelAppliedDirectiveCoordinates {
            return parent.coordinates().appliedDirective(node.name)
        }
    }

    companion object {
        fun from(type: GraphQLNamedType): NadelSchemaTraverserElement {
            return type.whenType(
                enumType = ::EnumType,
                inputObjectType = ::InputObjectType,
                interfaceType = ::InterfaceType,
                objectType = ::ObjectType,
                scalarType = ::ScalarType,
                unionType = ::UnionType,
            )
        }

        fun from(type: GraphQLDirective): NadelSchemaTraverserElement {
            return Directive(type)
        }
    }
}
