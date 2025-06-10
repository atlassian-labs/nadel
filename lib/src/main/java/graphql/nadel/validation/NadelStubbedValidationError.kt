package graphql.nadel.validation

import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLNamedSchemaElement

data class NadelStubbedOnNonNullFieldError(
    val type: NadelServiceSchemaElement.FieldsContainer,
    val field: GraphQLFieldDefinition,
) : NadelSchemaValidationError {
    override val message: String =
        "Field ${type.overall.name}.${field.name} used @stubbed so its output type must be nullable"
    override val subject: GraphQLNamedSchemaElement = field
}

data class NadelStubbedMustBeUsedExclusively(
    val type: NadelServiceSchemaElement.FieldsContainer,
    val field: GraphQLFieldDefinition,
) : NadelSchemaValidationError {
    override val message: String =
        "Field ${type.overall.name}.${field.name} used @stubbed with an incompatible @transform"
    override val subject: GraphQLNamedSchemaElement = field
}
