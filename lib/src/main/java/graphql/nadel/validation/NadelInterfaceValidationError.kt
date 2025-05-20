package graphql.nadel.validation

import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLNamedSchemaElement

sealed interface NadelInterfaceValidationError : NadelSchemaValidationError

data class NadelTypeMissingInterfaceError(
    val implementingType: NadelServiceSchemaElement.ImplementingType,
    val missingInterface: GraphQLInterfaceType,
) : NadelInterfaceValidationError {
    override val subject: GraphQLNamedSchemaElement = implementingType.overall

    override val message: String = run {
        val service = implementingType.service.name
        val offendingType = implementingType.overall.name
        val missingInterface = missingInterface.name
        "Type $offendingType does not implement interface $missingInterface in underlying schema in service $service"
    }
}
