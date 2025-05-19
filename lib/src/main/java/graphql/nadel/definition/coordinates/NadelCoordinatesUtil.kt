package graphql.nadel.definition.coordinates

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

fun GraphQLUnionType.coordinates(): NadelUnionCoordinates {
    return NadelUnionCoordinates(name)
}

fun GraphQLInterfaceType.coordinates(): NadelInterfaceCoordinates {
    return NadelInterfaceCoordinates(name)
}

fun GraphQLEnumType.coordinates(): NadelEnumCoordinates {
    return NadelEnumCoordinates(name)
}

fun GraphQLInputObjectType.coordinates(): NadelInputObjectCoordinates {
    return NadelInputObjectCoordinates(name)
}

fun GraphQLObjectType.coordinates(): NadelObjectCoordinates {
    return NadelObjectCoordinates(name)
}

fun GraphQLScalarType.coordinates(): NadelScalarCoordinates {
    return NadelScalarCoordinates(name)
}

fun GraphQLDirective.coordinates(): NadelDirectiveCoordinates {
    return NadelDirectiveCoordinates(name)
}

fun GraphQLFieldsContainer.coordinates(): NadelFieldContainerCoordinates {
    return whenType(
        interfaceType = GraphQLInterfaceType::coordinates,
        objectType = GraphQLObjectType::coordinates,
    )
}

fun GraphQLNamedType.coordinates(): NadelTypeCoordinates {
    return whenType(
        enumType = GraphQLEnumType::coordinates,
        inputObjectType = GraphQLInputObjectType::coordinates,
        interfaceType = GraphQLInterfaceType::coordinates,
        objectType = GraphQLObjectType::coordinates,
        scalarType = GraphQLScalarType::coordinates,
        unionType = GraphQLUnionType::coordinates,
    )
}

internal fun NadelSchemaTraverserElement.Type.coordinates(): NadelTypeCoordinates {
    return when (this) {
        is NadelSchemaTraverserElement.EnumType -> NadelEnumCoordinates(node.name)
        is NadelSchemaTraverserElement.InputObjectType -> NadelInputObjectCoordinates(node.name)
        is NadelSchemaTraverserElement.ScalarType -> NadelScalarCoordinates(node.name)
        is NadelSchemaTraverserElement.InterfaceType -> NadelInterfaceCoordinates(node.name)
        is NadelSchemaTraverserElement.ObjectType -> NadelObjectCoordinates(node.name)
        is NadelSchemaTraverserElement.UnionType -> NadelUnionCoordinates(node.name)
    }
}
