package graphql.nadel.definition

import graphql.nadel.engine.blueprint.NadelSchemaTraverserElement
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

    val parentOrNull: NadelSchemaMemberCoordinates?
        get() = (this as? ChildCoordinates)?.parent

    fun startsWith(other: NadelSchemaMemberCoordinates): Boolean {
        var parent = this

        while (true) {
            if (parent == other) {
                return true
            }

            parent = parent.parentOrNull ?: return false
        }
    }

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

    sealed interface ChildCoordinates : NadelSchemaMemberCoordinates {
        val parent: NadelSchemaMemberCoordinates
    }

    data class Object(
        override val name: String,
    ) : NadelSchemaMemberCoordinates,
        Type,
        ImplementingType,
        AppliedDirectiveParent,
        FieldContainer {
        override fun toString(): String {
            return toHumanReadableString(this)
        }
    }

    data class Interface(
        override val name: String,
    ) : NadelSchemaMemberCoordinates,
        Type,
        ImplementingType,
        AppliedDirectiveParent,
        FieldContainer {
        override fun toString(): String {
            return toHumanReadableString(this)
        }
    }

    data class Scalar(
        override val name: String,
    ) : NadelSchemaMemberCoordinates,
        Type,
        AppliedDirectiveParent {
        override fun toString(): String {
            return toHumanReadableString(this)
        }
    }

    data class Enum(
        override val name: String,
    ) : NadelSchemaMemberCoordinates,
        Type,
        AppliedDirectiveParent {
        fun enumValue(name: String): EnumValue {
            return EnumValue(parent = this, name = name)
        }

        override fun toString(): String {
            return toHumanReadableString(this)
        }
    }

    data class EnumValue(
        override val parent: NadelSchemaMemberCoordinates,
        override val name: String,
    ) : NadelSchemaMemberCoordinates,
        ChildCoordinates,
        AppliedDirectiveParent {
        override fun toString(): String {
            return toHumanReadableString(this)
        }
    }

    data class Union(
        override val name: String,
    ) : NadelSchemaMemberCoordinates,
        Type,
        AppliedDirectiveParent {
        override fun toString(): String {
            return toHumanReadableString(this)
        }
    }

    data class InputObject(
        override val name: String,
    ) : NadelSchemaMemberCoordinates,
        Type,
        AppliedDirectiveParent {
        fun field(name: String): InputObjectField {
            return InputObjectField(parent = this, name = name)
        }

        override fun toString(): String {
            return toHumanReadableString(this)
        }
    }

    data class Directive(
        override val name: String,
    ) : NadelSchemaMemberCoordinates,
        Type,
        ArgumentParent {
        override fun toString(): String {
            return toHumanReadableString(this)
        }
    }

    data class Field(
        override val parent: FieldContainer,
        override val name: String,
    ) : NadelSchemaMemberCoordinates,
        ChildCoordinates,
        ArgumentParent,
        AppliedDirectiveParent {
        override fun toString(): String {
            return toHumanReadableString(this)
        }
    }

    data class Argument(
        override val parent: ArgumentParent,
        override val name: String,
    ) : NadelSchemaMemberCoordinates,
        ChildCoordinates,
        AppliedDirectiveParent,
        DefaultValueHolder {
        override fun toString(): String {
            return toHumanReadableString(this)
        }
    }

    data class AppliedDirective(
        override val parent: AppliedDirectiveParent,
        override val name: String,
    ) : NadelSchemaMemberCoordinates,
        ChildCoordinates {
        fun argument(name: String): AppliedDirectiveArgument {
            return AppliedDirectiveArgument(parent = this, name = name)
        }

        override fun toString(): String {
            return toHumanReadableString(this)
        }
    }

    data class AppliedDirectiveArgument(
        override val parent: AppliedDirective,
        override val name: String,
    ) : NadelSchemaMemberCoordinates,
        ChildCoordinates {
        override fun toString(): String {
            return toHumanReadableString(this)
        }
    }

    data class InputObjectField(
        override val parent: InputObject,
        override val name: String,
    ) : NadelSchemaMemberCoordinates,
        ChildCoordinates,
        AppliedDirectiveParent,
        DefaultValueHolder {
        override fun toString(): String {
            return toHumanReadableString(this)
        }
    }

    companion object {
        fun toHumanReadableString(coordinates: NadelSchemaMemberCoordinates): String {
            val components = mutableListOf<String>()

            var cursor: NadelSchemaMemberCoordinates? = coordinates
            while (cursor != null) {
                components.add("${cursor.name} (${cursor.javaClass.simpleName})")
                cursor = (cursor as? ChildCoordinates)?.parent
            }

            return components.asReversed().joinToString(".")
        }
    }
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

internal fun NadelSchemaTraverserElement.Type.coordinates(): NadelSchemaMemberCoordinates.Type {
    return when (this) {
        is NadelSchemaTraverserElement.EnumType -> NadelSchemaMemberCoordinates.Enum(node.name)
        is NadelSchemaTraverserElement.InputObjectType -> NadelSchemaMemberCoordinates.InputObject(node.name)
        is NadelSchemaTraverserElement.ScalarType -> NadelSchemaMemberCoordinates.Scalar(node.name)
        is NadelSchemaTraverserElement.InterfaceType -> NadelSchemaMemberCoordinates.Interface(node.name)
        is NadelSchemaTraverserElement.ObjectType -> NadelSchemaMemberCoordinates.Object(node.name)
        is NadelSchemaTraverserElement.UnionType -> NadelSchemaMemberCoordinates.Union(node.name)
    }
}
