/**
 * Util functions to treat some GraphQL Java classes _like_ sealed types.
 *
 * There are tests in `NadelPseudoSealedTypeKtTest` to ensure these assumptions are correct.
 */
package graphql.nadel.engine.util

import graphql.language.DirectiveDefinition
import graphql.language.EnumTypeDefinition
import graphql.language.EnumTypeExtensionDefinition
import graphql.language.InputObjectTypeDefinition
import graphql.language.InputObjectTypeExtensionDefinition
import graphql.language.InterfaceTypeDefinition
import graphql.language.InterfaceTypeExtensionDefinition
import graphql.language.ListType
import graphql.language.NonNullType
import graphql.language.ObjectTypeDefinition
import graphql.language.ObjectTypeExtensionDefinition
import graphql.language.ScalarTypeDefinition
import graphql.language.ScalarTypeExtensionDefinition
import graphql.language.SchemaDefinition
import graphql.language.SchemaExtensionDefinition
import graphql.language.Type
import graphql.language.TypeName
import graphql.language.UnionTypeDefinition
import graphql.language.UnionTypeExtensionDefinition
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLList
import graphql.schema.GraphQLNamedInputType
import graphql.schema.GraphQLNamedOutputType
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLNonNull
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLType
import graphql.schema.GraphQLUnionType
import graphql.schema.GraphQLUnmodifiedType

inline fun <T> GraphQLFieldsContainer.whenType(
    interfaceType: (GraphQLInterfaceType) -> T,
    objectType: (GraphQLObjectType) -> T,
): T {
    return when (this) {
        is GraphQLInterfaceType -> interfaceType(this)
        is GraphQLObjectType -> objectType(this)
        else -> throw IllegalStateException("Should never happen")
    }
}

inline fun <T> GraphQLNamedInputType.whenType(
    enumType: (GraphQLEnumType) -> T,
    inputObjectType: (GraphQLInputObjectType) -> T,
    scalarType: (GraphQLScalarType) -> T,
): T {
    return when (this) {
        is GraphQLEnumType -> enumType(this)
        is GraphQLInputObjectType -> inputObjectType(this)
        is GraphQLScalarType -> scalarType(this)
        else -> throw IllegalStateException("Should never happen")
    }
}

inline fun <T> GraphQLNamedOutputType.whenType(
    enumType: (GraphQLEnumType) -> T,
    interfaceType: (GraphQLInterfaceType) -> T,
    objectType: (GraphQLObjectType) -> T,
    scalarType: (GraphQLScalarType) -> T,
    unionType: (GraphQLUnionType) -> T,
): T {
    return when (this) {
        is GraphQLEnumType -> enumType(this)
        is GraphQLInterfaceType -> interfaceType(this)
        is GraphQLObjectType -> objectType(this)
        is GraphQLScalarType -> scalarType(this)
        is GraphQLUnionType -> unionType(this)
        else -> throw IllegalStateException("Should never happen")
    }
}

inline fun <T> GraphQLNamedType.whenType(
    enumType: (GraphQLEnumType) -> T,
    inputObjectType: (GraphQLInputObjectType) -> T,
    interfaceType: (GraphQLInterfaceType) -> T,
    objectType: (GraphQLObjectType) -> T,
    scalarType: (GraphQLScalarType) -> T,
    unionType: (GraphQLUnionType) -> T,
): T {
    return when (this) {
        is GraphQLEnumType -> enumType(this)
        is GraphQLInputObjectType -> inputObjectType(this)
        is GraphQLInterfaceType -> interfaceType(this)
        is GraphQLObjectType -> objectType(this)
        is GraphQLScalarType -> scalarType(this)
        is GraphQLUnionType -> unionType(this)
        else -> throw IllegalStateException("Should never happen")
    }
}

inline fun <T> GraphQLOutputType.whenUnmodifiedType(
    enumType: (GraphQLEnumType) -> T,
    interfaceType: (GraphQLInterfaceType) -> T,
    objectType: (GraphQLObjectType) -> T,
    scalarType: (GraphQLScalarType) -> T,
    unionType: (GraphQLUnionType) -> T,
): T {
    return when (val unmodifiedType = this.unwrapAll()) {
        is GraphQLEnumType -> enumType(unmodifiedType)
        is GraphQLInterfaceType -> interfaceType(unmodifiedType)
        is GraphQLObjectType -> objectType(unmodifiedType)
        is GraphQLScalarType -> scalarType(unmodifiedType)
        is GraphQLUnionType -> unionType(unmodifiedType)
        else -> throw IllegalStateException("Should never happen")
    }
}

inline fun <T> GraphQLInputType.whenUnmodifiedType(
    enumType: (GraphQLEnumType) -> T,
    inputObjectType: (GraphQLInputObjectType) -> T,
    scalarType: (GraphQLScalarType) -> T,
): T {
    return when (val unmodifiedType = this.unwrapAll()) {
        is GraphQLEnumType -> enumType(unmodifiedType)
        is GraphQLInputObjectType -> inputObjectType(unmodifiedType)
        is GraphQLScalarType -> scalarType(unmodifiedType)
        else -> throw IllegalStateException("Should never happen")
    }
}

inline fun <T> GraphQLType.whenType(
    listType: (GraphQLList) -> T,
    nonNull: (GraphQLNonNull) -> T,
    unmodifiedType: (GraphQLUnmodifiedType) -> T,
): T {
    return when (this) {
        is GraphQLList -> listType(this)
        is GraphQLNonNull -> nonNull(this)
        is GraphQLUnmodifiedType -> unmodifiedType(this)
        else -> throw IllegalStateException("Should never happen")
    }
}

inline fun <T> Type<*>.whenType(
    listType: (ListType) -> T,
    nonNull: (NonNullType) -> T,
    unmodifiedType: (TypeName) -> T,
): T {
    return when (this) {
        is ListType -> listType(this)
        is NonNullType -> nonNull(this)
        is TypeName -> unmodifiedType(this)
        else -> throw IllegalStateException("Should never happen")
    }
}

inline fun <T> AnySDLDefinition.whenType(
    directiveDefinition: (DirectiveDefinition) -> T,
    enumTypeDefinition: (EnumTypeDefinition) -> T,
    enumTypeExtensionDefinition: (EnumTypeExtensionDefinition) -> T,
    inputObjectTypeDefinition: (InputObjectTypeDefinition) -> T,
    inputObjectTypeExtensionDefinition: (InputObjectTypeExtensionDefinition) -> T,
    interfaceTypeDefinition: (InterfaceTypeDefinition) -> T,
    interfaceTypeExtensionDefinition: (InterfaceTypeExtensionDefinition) -> T,
    objectTypeDefinition: (ObjectTypeDefinition) -> T,
    objectTypeExtensionDefinition: (ObjectTypeExtensionDefinition) -> T,
    scalarTypeDefinition: (ScalarTypeDefinition) -> T,
    scalarTypeExtensionDefinition: (ScalarTypeExtensionDefinition) -> T,
    schemaDefinition: (SchemaDefinition) -> T,
    schemaExtensionDefinition: (SchemaExtensionDefinition) -> T,
    unionTypeDefinition: (UnionTypeDefinition) -> T,
    unionTypeExtensionDefinition: (UnionTypeExtensionDefinition) -> T,
): T {
    return when (this) {
        is DirectiveDefinition -> directiveDefinition(this)
        is EnumTypeExtensionDefinition -> enumTypeExtensionDefinition(this)
        is EnumTypeDefinition -> enumTypeDefinition(this)
        is InputObjectTypeExtensionDefinition -> inputObjectTypeExtensionDefinition(this)
        is InputObjectTypeDefinition -> inputObjectTypeDefinition(this)
        is InterfaceTypeExtensionDefinition -> interfaceTypeExtensionDefinition(this)
        is InterfaceTypeDefinition -> interfaceTypeDefinition(this)
        is ObjectTypeExtensionDefinition -> objectTypeExtensionDefinition(this)
        is ObjectTypeDefinition -> objectTypeDefinition(this)
        is ScalarTypeExtensionDefinition -> scalarTypeExtensionDefinition(this)
        is ScalarTypeDefinition -> scalarTypeDefinition(this)
        is SchemaExtensionDefinition -> schemaExtensionDefinition(this)
        is SchemaDefinition -> schemaDefinition(this)
        is UnionTypeExtensionDefinition -> unionTypeExtensionDefinition(this)
        is UnionTypeDefinition -> unionTypeDefinition(this)
        else -> throw IllegalStateException("Should never happen")
    }
}
