package graphql.nadel.validation

import graphql.nadel.Service
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLNamedSchemaElement
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLUnionType

sealed class NadelServiceSchemaElement {
    abstract val service: Service
    abstract val overall: GraphQLNamedSchemaElement
    abstract val underlying: GraphQLNamedSchemaElement

    internal fun toRef() = NadelServiceSchemaElementRef(service, overall, underlying)

    sealed class Type : NadelServiceSchemaElement() {
        abstract override val overall: GraphQLNamedType
        abstract override val underlying: GraphQLNamedType
    }

    sealed class FieldsContainer : Type() {
        abstract override val overall: GraphQLFieldsContainer
        abstract override val underlying: GraphQLFieldsContainer
    }

    data class Interface(
        override val service: Service,
        override val overall: GraphQLInterfaceType,
        override val underlying: GraphQLInterfaceType,
    ) : FieldsContainer()

    data class Object(
        override val service: Service,
        override val overall: GraphQLObjectType,
        override val underlying: GraphQLObjectType,
    ) : FieldsContainer()

    data class Union(
        override val service: Service,
        override val overall: GraphQLUnionType,
        override val underlying: GraphQLUnionType,
    ) : Type()

    data class Enum(
        override val service: Service,
        override val overall: GraphQLEnumType,
        override val underlying: GraphQLEnumType,
    ) : Type()

    data class Scalar(
        override val service: Service,
        override val overall: GraphQLScalarType,
        override val underlying: GraphQLScalarType,
    ) : Type()

    data class InputObject(
        override val service: Service,
        override val overall: GraphQLInputObjectType,
        override val underlying: GraphQLInputObjectType,
    ) : Type()

    data class Incompatible(
        override val service: Service,
        override val overall: GraphQLNamedSchemaElement,
        override val underlying: GraphQLNamedSchemaElement,
    ) : NadelServiceSchemaElement()

    companion object {
        fun from(
            service: Service,
            overall: GraphQLNamedType,
            underlying: GraphQLNamedType,
        ): NadelServiceSchemaElement {
            return when {
                overall is GraphQLInterfaceType && underlying is GraphQLInterfaceType -> {
                    Interface(service, overall, underlying)
                }
                overall is GraphQLObjectType && underlying is GraphQLObjectType -> {
                    Object(service, overall, underlying)
                }
                overall is GraphQLUnionType && underlying is GraphQLUnionType -> {
                    Union(service, overall, underlying)
                }
                overall is GraphQLEnumType && underlying is GraphQLEnumType -> {
                    Enum(service, overall, underlying)
                }
                overall is GraphQLScalarType && underlying is GraphQLScalarType -> {
                    Scalar(service, overall, underlying)
                }
                overall is GraphQLInputObjectType && underlying is GraphQLInputObjectType -> {
                    InputObject(service, overall, underlying)
                }
                else -> {
                    Incompatible(service, overall, underlying)
                }
            }
        }
    }
}

/**
 * This is used to create a version of [NadelServiceSchemaElement] that has a proper
 * [hashCode] definition instead of relying on identity hashCodes.
 */
internal data class NadelServiceSchemaElementRef(
    val service: String,
    val overall: String,
    val underlying: String,
) {
    companion object {
        operator fun invoke(
            service: Service,
            overall: GraphQLNamedSchemaElement,
            underlying: GraphQLNamedSchemaElement,
        ) = NadelServiceSchemaElementRef(service.name, overall.name, underlying.name)
    }
}

