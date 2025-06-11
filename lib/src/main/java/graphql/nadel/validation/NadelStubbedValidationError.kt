package graphql.nadel.validation

import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLNamedSchemaElement
import graphql.schema.GraphQLObjectType

data class NadelStubbedMustBeNullableError(
    val parent: NadelServiceSchemaElement.FieldsContainer,
    val field: GraphQLFieldDefinition,
) : NadelSchemaValidationError {
    override val message: String =
        "Field ${parent.overall.name}.${field.name} is @stubbed so its output type must be nullable"
    override val subject: GraphQLNamedSchemaElement = field
}

data class NadelStubbedMustBeUsedExclusively(
    val parent: NadelServiceSchemaElement.FieldsContainer,
    val field: GraphQLFieldDefinition,
) : NadelSchemaValidationError {
    override val message: String =
        "Field ${parent.overall.name}.${field.name} is @stubbed with an incompatible @transform"
    override val subject: GraphQLNamedSchemaElement = field
}

data class NadelStubbedTypeMustNotImplementError(
    val type: NadelServiceSchemaElement.StubbedType,
) : NadelSchemaValidationError {
    override val message: String = "Type ${type.overall.name} is @stubbed and so it cannot implement any interfaces"
    override val subject: GraphQLNamedSchemaElement = type.overall
}

data class NadelStubbedMissingOnConcreteType(
    val interfaceType: NadelServiceSchemaElement.Interface,
    val objectType: GraphQLObjectType,
    val objectField: GraphQLFieldDefinition,
) : NadelSchemaValidationError {
    override val message: String =
        "Field ${objectField.name} was @stubbed on interface ${interfaceType.overall.name} but not on object type ${objectType.name}"
    override val subject: GraphQLNamedSchemaElement = objectField
}
