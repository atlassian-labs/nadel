package graphql.nadel.schema

internal interface NadelSchemaDefinitionTraverserVisitor {
    fun visitGraphQLArgument(
        element: NadelSchemaDefinitionTraverserElement.Argument,
    ): Boolean

    fun visitGraphQLUnionType(
        element: NadelSchemaDefinitionTraverserElement.UnionType,
    ): Boolean

    fun visitGraphQLInterfaceType(
        element: NadelSchemaDefinitionTraverserElement.InterfaceType,
    ): Boolean

    fun visitGraphQLEnumType(
        element: NadelSchemaDefinitionTraverserElement.EnumType,
    ): Boolean

    fun visitGraphQLEnumValueDefinition(
        element: NadelSchemaDefinitionTraverserElement.EnumValueDefinition,
    ): Boolean

    fun visitGraphQLFieldDefinition(
        element: NadelSchemaDefinitionTraverserElement.FieldDefinition,
    ): Boolean

    fun visitGraphQLInputObjectField(
        element: NadelSchemaDefinitionTraverserElement.InputObjectField,
    ): Boolean

    fun visitGraphQLInputObjectType(
        element: NadelSchemaDefinitionTraverserElement.InputObjectType,
    ): Boolean

    fun visitGraphQLObjectType(
        element: NadelSchemaDefinitionTraverserElement.ObjectType,
    ): Boolean

    fun visitGraphQLScalarType(
        element: NadelSchemaDefinitionTraverserElement.ScalarType,
    ): Boolean

    fun visitGraphQLDirective(
        element: NadelSchemaDefinitionTraverserElement.Directive,
    ): Boolean

    fun visitGraphQLAppliedDirective(
        element: NadelSchemaDefinitionTraverserElement.AppliedDirective,
    ): Boolean

    fun visitGraphQLAppliedDirectiveArgument(
        element: NadelSchemaDefinitionTraverserElement.AppliedDirectiveArgument,
    ): Boolean

    fun visitTypeReference(
        element: NadelSchemaDefinitionTraverserElement.TypeReference,
    ): Boolean
}
