package graphql.nadel.validation

import graphql.nadel.Service
import graphql.schema.GraphQLNamedSchemaElement
import graphql.schema.GraphQLSchema

class NadelSchemaValidation(
    private val overallSchema: GraphQLSchema,
    private val services: Map<String, Service>,
) {
    fun validate(): Set<NadelSchemaValidationError> {
        val context = NadelValidationContext()
        val typeValidation = NadelTypeValidation(context, overallSchema, services)
        return services
            .asSequence()
            .flatMap { (_, service) ->
                typeValidation.validate(service)
            }
            .toSet()
    }
}

data class NadelServiceSchemaElement(
    val service: Service,
    val overall: GraphQLNamedSchemaElement,
    val underlying: GraphQLNamedSchemaElement,
) {
    internal fun toRef() = NadelServiceSchemaElementRef(service, overall, underlying)
}

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
