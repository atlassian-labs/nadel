package graphql.nadel.validation

import graphql.nadel.Service
import graphql.schema.GraphQLNamedSchemaElement
import graphql.schema.GraphQLSchema

class NadelSchemaValidation(
    private val overallSchema: GraphQLSchema,
    private val services: Map<String, Service>,
) {
    fun getIssues(): List<NadelSchemaValidationError> {
        val context = NadelValidationContext()
        val typeValidation = NadelTypeValidation(context, overallSchema, services)
        return services.flatMap { (_, service) ->
            typeValidation.getIssues(service)
        }
    }
}

data class NadelServiceSchemaElement(
    val service: Service,
    val overall: GraphQLNamedSchemaElement,
    val underlying: GraphQLNamedSchemaElement,
) {
    fun toRef() = NadelServiceSchemaElementRef(service, overall, underlying)
}

data class NadelServiceSchemaElementRef(
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
