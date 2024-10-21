package graphql.nadel.util

import graphql.language.Definition
import graphql.language.EnumTypeExtensionDefinition
import graphql.language.ImplementingTypeDefinition
import graphql.language.InputObjectTypeExtensionDefinition
import graphql.language.InterfaceTypeExtensionDefinition
import graphql.language.NamedNode
import graphql.language.Node
import graphql.language.ObjectField
import graphql.language.ObjectTypeExtensionDefinition
import graphql.language.ObjectValue
import graphql.language.SDLDefinition
import graphql.language.SDLNamedDefinition
import graphql.language.ScalarTypeExtensionDefinition
import graphql.language.SchemaExtensionDefinition
import graphql.language.Type
import graphql.language.UnionTypeExtensionDefinition
import graphql.language.Value

// this is duped from engine-nextgen/ so we need to dedup it at some point

internal typealias AnyAstValue = Value<*>
internal typealias AnyAstNode = Node<*>
internal typealias AnyAstDefinition = Definition<*>
internal typealias AnyImplementingTypeDefinition = ImplementingTypeDefinition<*>
internal typealias AnyNamedNode = NamedNode<*>
internal typealias AnySDLDefinition = SDLDefinition<*>
internal typealias AnyAstType = Type<*>
internal typealias AnySDLNamedDefinition = SDLNamedDefinition<*>

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

internal fun ObjectValue.getObjectField(name: String): ObjectField {
    return objectFields.first { it.name == name }
}
