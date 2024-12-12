package graphql.nadel.definition

import graphql.nadel.engine.util.whenType
import graphql.schema.GraphQLDirective
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLUnionType

sealed interface NadelSchemaMemberCoordinates {
    val name: String

    sealed interface ArgumentParent : NadelSchemaMemberCoordinates {
        fun argument(name: String): Argument {
            return Argument(parent = this, name = name)
        }
    }

    sealed interface AppliedDirectiveParent : NadelSchemaMemberCoordinates {
        fun appliedDirective(name: String): AppliedDirective {
            return AppliedDirective(parent = this, name = name)
        }
    }

    sealed interface FieldContainer : NadelSchemaMemberCoordinates {
        fun field(name: String): Field {
            return Field(parent = this, name = name)
        }
    }

    sealed interface DefaultValueHolder : NadelSchemaMemberCoordinates
    sealed interface Type : NadelSchemaMemberCoordinates
    sealed interface ImplementingType : NadelSchemaMemberCoordinates

    sealed interface ChildCoordinates {
        val parent: NadelSchemaMemberCoordinates
    }

    data class Object(
        override val name: String,
    ) : NadelSchemaMemberCoordinates,
        Type,
        ImplementingType,
        AppliedDirectiveParent,
        FieldContainer

    data class Interface(
        override val name: String,
    ) : NadelSchemaMemberCoordinates,
        Type,
        ImplementingType,
        AppliedDirectiveParent,
        FieldContainer

    data class Scalar(
        override val name: String,
    ) : NadelSchemaMemberCoordinates,
        Type,
        AppliedDirectiveParent

    data class Enum(
        override val name: String,
    ) : NadelSchemaMemberCoordinates,
        Type,
        AppliedDirectiveParent

    data class Union(
        override val name: String,
    ) : NadelSchemaMemberCoordinates,
        Type,
        AppliedDirectiveParent

    data class InputObject(
        override val name: String,
    ) : NadelSchemaMemberCoordinates,
        Type,
        AppliedDirectiveParent {
        fun field(name: String): InputObjectField {
            return InputObjectField(parent = this, name = name)
        }
    }

    data class Directive(
        override val name: String,
    ) : NadelSchemaMemberCoordinates,
        Type,
        ArgumentParent

    data class Field(
        override val parent: FieldContainer,
        override val name: String,
    ) : NadelSchemaMemberCoordinates,
        ChildCoordinates,
        ArgumentParent,
        AppliedDirectiveParent

    data class Argument(
        override val parent: ArgumentParent,
        override val name: String,
    ) : NadelSchemaMemberCoordinates,
        ChildCoordinates,
        AppliedDirectiveParent,
        DefaultValueHolder

    data class AppliedDirective(
        override val parent: AppliedDirectiveParent,
        override val name: String,
    ) : NadelSchemaMemberCoordinates,
        ChildCoordinates,
        ArgumentParent

    data class InputObjectField(
        override val parent: InputObject,
        override val name: String,
    ) : NadelSchemaMemberCoordinates,
        ChildCoordinates,
        AppliedDirectiveParent,
        DefaultValueHolder
}

fun GraphQLUnionType.coordinates(): NadelSchemaMemberCoordinates.Union {
    return NadelSchemaMemberCoordinates.Union(name)
}

fun GraphQLInterfaceType.coordinates(): NadelSchemaMemberCoordinates.Interface {
    return NadelSchemaMemberCoordinates.Interface(name)
}

fun GraphQLEnumType.coordinates(): NadelSchemaMemberCoordinates.Enum {
    return NadelSchemaMemberCoordinates.Enum(name)
}

fun GraphQLInputObjectType.coordinates(): NadelSchemaMemberCoordinates.InputObject {
    return NadelSchemaMemberCoordinates.InputObject(name)
}

fun GraphQLObjectType.coordinates(): NadelSchemaMemberCoordinates.Object {
    return NadelSchemaMemberCoordinates.Object(name)
}

fun GraphQLScalarType.coordinates(): NadelSchemaMemberCoordinates.Scalar {
    return NadelSchemaMemberCoordinates.Scalar(name)
}

fun GraphQLDirective.coordinates(): NadelSchemaMemberCoordinates.Directive {
    return NadelSchemaMemberCoordinates.Directive(name)
}

fun GraphQLFieldsContainer.coordinates(): NadelSchemaMemberCoordinates.FieldContainer {
    return whenType(
        interfaceType = GraphQLInterfaceType::coordinates,
        objectType = GraphQLObjectType::coordinates,
    )
}

fun GraphQLNamedType.coordinates(): NadelSchemaMemberCoordinates.Type {
    return whenType(
        enumType = GraphQLEnumType::coordinates,
        inputObjectType = GraphQLInputObjectType::coordinates,
        interfaceType = GraphQLInterfaceType::coordinates,
        objectType = GraphQLObjectType::coordinates,
        scalarType = GraphQLScalarType::coordinates,
        unionType = GraphQLUnionType::coordinates,
    )
}
