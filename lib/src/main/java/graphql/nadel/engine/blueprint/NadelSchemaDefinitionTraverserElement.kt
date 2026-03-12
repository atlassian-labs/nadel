package graphql.nadel.engine.blueprint

import graphql.language.DirectiveDefinition
import graphql.language.EnumTypeDefinition
import graphql.language.ImplementingTypeDefinition
import graphql.language.InputObjectTypeDefinition
import graphql.language.InputValueDefinition
import graphql.language.InterfaceTypeDefinition
import graphql.language.Node
import graphql.language.ObjectTypeDefinition
import graphql.language.ScalarTypeDefinition
import graphql.language.TypeDefinition
import graphql.language.UnionTypeDefinition
import graphql.nadel.engine.util.AnySDLDefinition
import graphql.nadel.engine.util.whenType

internal sealed interface NadelSchemaDefinitionTraverserElement {
    val parent: NadelSchemaDefinitionTraverserElement?

    val node: Node<*>

    fun forEachChild(onElement: (NadelSchemaDefinitionTraverserElement) -> Unit)

    sealed interface Type : NadelSchemaDefinitionTraverserElement {
        override val node: TypeDefinition<*>
    }

    sealed interface FieldsContainer : NadelSchemaDefinitionTraverserElement {
        override val node: ImplementingTypeDefinition<*>
    }

    sealed interface AppliedDirectiveParent : NadelSchemaDefinitionTraverserElement

    sealed interface ArgumentParent : NadelSchemaDefinitionTraverserElement

    data class TypeReference(
        override val parent: NadelSchemaDefinitionTraverserElement,
        override val node: graphql.language.Type<*>,
    ) : NadelSchemaDefinitionTraverserElement {
        override fun forEachChild(onElement: (NadelSchemaDefinitionTraverserElement) -> Unit) {
        }
    }

    sealed interface Argument : NadelSchemaDefinitionTraverserElement, AppliedDirectiveParent {
        override val parent: ArgumentParent

        override val node: InputValueDefinition

        override fun forEachChild(onElement: (NadelSchemaDefinitionTraverserElement) -> Unit) {
            onElement(TypeReference(this, node.type))

            node.directives.forEach { directive ->
                onElement(AppliedDirective(this, directive))
            }
        }
    }

    sealed interface OutputType : NadelSchemaDefinitionTraverserElement, Type

    sealed interface InputType : NadelSchemaDefinitionTraverserElement, Type

    data class FieldArgument(
        override val parent: FieldDefinition,
        override val node: InputValueDefinition,
    ) : Argument

    data class DirectiveArgument(
        override val parent: Directive,
        override val node: InputValueDefinition,
    ) : Argument

    data class AppliedDirectiveArgument(
        override val parent: AppliedDirective,
        override val node: graphql.language.Argument,
    ) : NadelSchemaDefinitionTraverserElement {
        override fun forEachChild(onElement: (NadelSchemaDefinitionTraverserElement) -> Unit) {
        }
    }

    data class UnionType(
        override val node: UnionTypeDefinition,
    ) : NadelSchemaDefinitionTraverserElement, Type, OutputType, AppliedDirectiveParent {
        override val parent: NadelSchemaDefinitionTraverserElement? = null

        override fun forEachChild(onElement: (NadelSchemaDefinitionTraverserElement) -> Unit) {
            node.memberTypes.forEach { member ->
                onElement(TypeReference(this, member))
            }

            node.directives.forEach { directive ->
                onElement(AppliedDirective(this, directive))
            }
        }
    }

    data class InterfaceType(
        override val node: InterfaceTypeDefinition,
    ) : NadelSchemaDefinitionTraverserElement, Type, OutputType, FieldsContainer, AppliedDirectiveParent {
        override val parent: NadelSchemaDefinitionTraverserElement? = null

        override fun forEachChild(onElement: (NadelSchemaDefinitionTraverserElement) -> Unit) {
            node.fieldDefinitions.forEach { field ->
                onElement(FieldDefinition(this, field))
            }
            node.implements.forEach { parentType ->
                onElement(TypeReference(this, parentType))
            }
            node.directives.forEach { directive ->
                onElement(AppliedDirective(this, directive))
            }
        }
    }

    data class EnumType(
        override val node: EnumTypeDefinition,
    ) : NadelSchemaDefinitionTraverserElement, Type, InputType, OutputType, AppliedDirectiveParent {
        override val parent: NadelSchemaDefinitionTraverserElement? = null

        override fun forEachChild(onElement: (NadelSchemaDefinitionTraverserElement) -> Unit) {
            node.enumValueDefinitions.forEach { value ->
                onElement(EnumValueDefinition(this, value))
            }

            node.directives.forEach { directive ->
                onElement(AppliedDirective(this, directive))
            }
        }
    }

    data class EnumValueDefinition(
        override val parent: EnumType,
        override val node: graphql.language.EnumValueDefinition,
    ) : NadelSchemaDefinitionTraverserElement, AppliedDirectiveParent {
        override fun forEachChild(onElement: (NadelSchemaDefinitionTraverserElement) -> Unit) {
            node.directives.forEach { directive ->
                onElement(AppliedDirective(this, directive))
            }
        }
    }

    data class FieldDefinition(
        override val parent: FieldsContainer,
        override val node: graphql.language.FieldDefinition,
    ) : NadelSchemaDefinitionTraverserElement, ArgumentParent, AppliedDirectiveParent {
        override fun forEachChild(onElement: (NadelSchemaDefinitionTraverserElement) -> Unit) {
            onElement(TypeReference(this, node.type))

            node.inputValueDefinitions.forEach { arg ->
                onElement(FieldArgument(this, arg))
            }

            node.directives.forEach { directive ->
                onElement(AppliedDirective(this, directive))
            }
        }
    }

    data class InputObjectField(
        override val parent: InputObjectType,
        override val node: InputValueDefinition,
    ) : NadelSchemaDefinitionTraverserElement, AppliedDirectiveParent {
        override fun forEachChild(onElement: (NadelSchemaDefinitionTraverserElement) -> Unit) {
            onElement(TypeReference(this, node.type))

            node.directives.forEach { directive ->
                onElement(AppliedDirective(this, directive))
            }
        }
    }

    data class InputObjectType(
        override val node: InputObjectTypeDefinition,
    ) : NadelSchemaDefinitionTraverserElement, Type, InputType, AppliedDirectiveParent {
        override val parent: NadelSchemaDefinitionTraverserElement? = null

        override fun forEachChild(onElement: (NadelSchemaDefinitionTraverserElement) -> Unit) {
            node.inputValueDefinitions.forEach { field ->
                onElement(InputObjectField(this, field))
            }

            node.directives.forEach { directive ->
                onElement(AppliedDirective(this, directive))
            }
        }
    }

    data class ObjectType(
        override val node: ObjectTypeDefinition,
    ) : NadelSchemaDefinitionTraverserElement, Type, OutputType, FieldsContainer, AppliedDirectiveParent {
        override val parent: NadelSchemaDefinitionTraverserElement? = null

        override fun forEachChild(onElement: (NadelSchemaDefinitionTraverserElement) -> Unit) {
            node.fieldDefinitions.forEach { field ->
                onElement(FieldDefinition(this, field))
            }

            node.implements.forEach { parentType ->
                onElement(TypeReference(this, parentType))
            }

            node.directives.forEach { directive ->
                onElement(AppliedDirective(this, directive))
            }
        }
    }

    data class SchemaDefinition(
        override val node: graphql.language.SchemaDefinition,
    ) : NadelSchemaDefinitionTraverserElement, AppliedDirectiveParent {
        override val parent: NadelSchemaDefinitionTraverserElement? = null

        override fun forEachChild(onElement: (NadelSchemaDefinitionTraverserElement) -> Unit) {
            node.operationTypeDefinitions.forEach {
                onElement(TypeReference(this, it.typeName))
            }

            node.directives.forEach { directive ->
                onElement(AppliedDirective(this, directive))
            }
        }
    }

    data class ScalarType(
        override val node: ScalarTypeDefinition,
    ) : NadelSchemaDefinitionTraverserElement, Type, InputType, OutputType, AppliedDirectiveParent {
        override val parent: NadelSchemaDefinitionTraverserElement? = null

        override fun forEachChild(onElement: (NadelSchemaDefinitionTraverserElement) -> Unit) {
            node.directives.forEach { directive ->
                onElement(AppliedDirective(this, directive))
            }
        }
    }

    data class Directive(
        override val node: DirectiveDefinition,
    ) : NadelSchemaDefinitionTraverserElement, ArgumentParent {
        override val parent: NadelSchemaDefinitionTraverserElement? = null

        override fun forEachChild(onElement: (NadelSchemaDefinitionTraverserElement) -> Unit) {
            node.inputValueDefinitions.forEach { arg ->
                onElement(DirectiveArgument(this, arg))
            }
        }
    }

    data class AppliedDirective(
        override val parent: AppliedDirectiveParent,
        override val node: graphql.language.Directive,
    ) : NadelSchemaDefinitionTraverserElement {
        override fun forEachChild(onElement: (NadelSchemaDefinitionTraverserElement) -> Unit) {
            node.arguments.forEach { arg ->
                onElement(AppliedDirectiveArgument(this, arg))
            }
        }
    }

    companion object {
        fun from(type: AnySDLDefinition): NadelSchemaDefinitionTraverserElement {
            return type.whenType(
                directiveDefinition = ::Directive,
                enumTypeDefinition = ::EnumType,
                enumTypeExtensionDefinition = ::EnumType,
                inputObjectTypeDefinition = ::InputObjectType,
                inputObjectTypeExtensionDefinition = ::InputObjectType,
                interfaceTypeDefinition = ::InterfaceType,
                interfaceTypeExtensionDefinition = ::InterfaceType,
                objectTypeDefinition = ::ObjectType,
                objectTypeExtensionDefinition = ::ObjectType,
                scalarTypeDefinition = ::ScalarType,
                scalarTypeExtensionDefinition = ::ScalarType,
                schemaDefinition = ::SchemaDefinition,
                schemaExtensionDefinition = ::SchemaDefinition,
                unionTypeDefinition = ::UnionType,
                unionTypeExtensionDefinition = ::UnionType,
            )
        }
    }
}
