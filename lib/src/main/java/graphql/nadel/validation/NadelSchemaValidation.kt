package graphql.nadel.validation

import graphql.nadel.NadelSchemas
import graphql.nadel.Service
import graphql.nadel.engine.util.strictAssociateBy
import graphql.schema.GraphQLNamedSchemaElement

class NadelSchemaValidation(private val schemas: NadelSchemas) {
    fun validate(): Set<NadelSchemaValidationError> {
        return validateAndGenerateBlueprint()
            .filterIsInstanceTo(LinkedHashSet())
    }

    fun validateAndGenerateBlueprint(): List<NadelSchemaValidationResult> {
        val (engineSchema, services) = schemas

        val servicesByName = services.strictAssociateBy(Service::name)
        val context = NadelValidationContext()
        val typeValidation = NadelTypeValidation(context, engineSchema, servicesByName)

        return services
            .flatMap(typeValidation::validate)
    }
}

data class NadelServiceSchemaElement(
    val service: Service,
    val overall: GraphQLNamedSchemaElement,
    val underlying: GraphQLNamedSchemaElement,
) {
    internal fun toRef() = NadelServiceSchemaElementRef(service, overall, underlying)
}

/**
 * This is used to create a version of [NadelServiceSchemaElement] that has a proper
 * [hashCode] definition instead of relying on identity hashCodes.
 */
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
