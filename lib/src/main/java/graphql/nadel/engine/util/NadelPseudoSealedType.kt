/**
 * Util functions to treat some GraphQL Java classes _like_ sealed types.
 *
 * There are tests in `NadelPseudoSealedTypeKtTest` to ensure these assumptions are correct.
 */
package graphql.nadel.engine.util

import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLUnionType

internal inline fun <T> GraphQLFieldsContainer.whenType(
    interfaceType: (GraphQLInterfaceType) -> T,
    objectType: (GraphQLObjectType) -> T,
): T {
    return when (this) {
        is GraphQLInterfaceType -> interfaceType(this)
        is GraphQLObjectType -> objectType(this)
        else -> throw IllegalStateException("Should never happen")
    }
}

internal inline fun <T> GraphQLNamedType.whenType(
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

internal inline fun <T> GraphQLOutputType.whenUnmodifiedType(
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

internal inline fun <T> GraphQLInputType.whenUnmodifiedType(
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

