package graphql.nadel.validation

import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLNamedOutputType
import graphql.schema.GraphQLNamedSchemaElement

data class NadelInvalidVirtualTypeError(
    val type: NadelServiceSchemaElement.VirtualType,
) : NadelSchemaValidationError {
    override val message: String = "Virtual type must be an object type"

    override val subject: GraphQLNamedSchemaElement
        get() = type.overall
}

data class NadelVirtualTypeUnexpectedFieldError(
    val type: NadelServiceSchemaElement.VirtualType,
    val virtualField: GraphQLFieldDefinition,
) : NadelSchemaValidationError {
    override val message: String = "Virtual type declares field that does not exist in backing type"

    override val subject: GraphQLNamedSchemaElement
        get() = type.overall
}

data class NadelVirtualTypeUnexpectedFieldArgumentError(
    val type: NadelServiceSchemaElement.VirtualType,
    val virtualField: GraphQLFieldDefinition,
    val backingField: GraphQLFieldDefinition,
    val virtualFieldArgument: GraphQLArgument,
) : NadelSchemaValidationError {
    override val message: String = "Virtual type declares field that does not exist in backing type"

    override val subject: GraphQLNamedSchemaElement
        get() = type.overall
}

data class NadelVirtualTypeIncompatibleFieldArgumentError(
    val type: NadelServiceSchemaElement.VirtualType,
    val virtualField: GraphQLFieldDefinition,
    val backingField: GraphQLFieldDefinition,
    val virtualFieldArgument: GraphQLArgument,
    val backingFieldArgument: GraphQLArgument,
) : NadelSchemaValidationError {
    override val message: String = "Virtual type declares field that does not exist in backing type"

    override val subject: GraphQLNamedSchemaElement
        get() = type.overall
}

data class NadelVirtualTypeRenameFieldError(
    val type: NadelServiceSchemaElement.VirtualType,
    val virtualField: GraphQLFieldDefinition,
) : NadelSchemaValidationError {
    override val message: String = "Virtual type declares @renamed field"

    override val subject: GraphQLNamedSchemaElement
        get() = type.overall
}

data class NadelVirtualTypeUnexpectedInterfaceError(
    val type: NadelServiceSchemaElement.VirtualType,
    val virtualFieldInterface: GraphQLNamedOutputType,
) : NadelSchemaValidationError {
    override val message: String = "Virtual type implements interface that does not exist in backing type"

    override val subject: GraphQLNamedSchemaElement
        get() = type.overall
}

data class NadelVirtualTypeIncompatibleFieldOutputTypeError(
    val parent: NadelServiceSchemaElement.VirtualType,
    val virtualField: GraphQLFieldDefinition,
    val backingField: GraphQLFieldDefinition,
) : NadelSchemaValidationError {
    override val message: String = "Virtual field output type does not match backing field's output type"

    override val subject: GraphQLNamedSchemaElement
        get() = parent.overall
}
