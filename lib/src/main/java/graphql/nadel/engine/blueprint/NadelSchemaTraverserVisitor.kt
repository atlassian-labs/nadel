package graphql.nadel.engine.blueprint

internal interface NadelSchemaTraverserVisitor {
    fun visitGraphQLArgument(
        element: NadelSchemaTraverserElement.Argument,
    ): Boolean

    fun visitGraphQLUnionType(
        element: NadelSchemaTraverserElement.UnionType,
    ): Boolean

    fun visitGraphQLUnionMemberType(
        element: NadelSchemaTraverserElement.UnionMemberType,
    ): Boolean

    fun visitGraphQLInterfaceType(
        element: NadelSchemaTraverserElement.InterfaceType,
    ): Boolean

    fun visitGraphQLEnumType(
        element: NadelSchemaTraverserElement.EnumType,
    ): Boolean

    fun visitGraphQLEnumValueDefinition(
        element: NadelSchemaTraverserElement.EnumValueDefinition,
    ): Boolean

    fun visitGraphQLFieldDefinition(
        element: NadelSchemaTraverserElement.FieldDefinition,
    ): Boolean

    fun visitGraphQLInputObjectField(
        element: NadelSchemaTraverserElement.InputObjectField,
    ): Boolean

    fun visitGraphQLInputObjectType(
        element: NadelSchemaTraverserElement.InputObjectType,
    ): Boolean

    fun visitGraphQLObjectType(
        element: NadelSchemaTraverserElement.ObjectType,
    ): Boolean

    fun visitGraphQLScalarType(
        element: NadelSchemaTraverserElement.ScalarType,
    ): Boolean

    fun visitGraphQLDirective(
        element: NadelSchemaTraverserElement.Directive,
    ): Boolean

    fun visitGraphQLAppliedDirective(
        element: NadelSchemaTraverserElement.AppliedDirective,
    ): Boolean

    fun visitGraphQLAppliedDirectiveArgument(
        element: NadelSchemaTraverserElement.AppliedDirectiveArgument,
    ): Boolean
}
