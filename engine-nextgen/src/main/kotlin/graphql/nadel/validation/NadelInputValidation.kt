package graphql.nadel.validation

import graphql.nadel.Service
import graphql.nadel.enginekt.util.strictAssociateBy
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingInputField
import graphql.schema.GraphQLInputObjectField
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLSchema

internal class NadelInputValidation(
    private val overallSchema: GraphQLSchema,
    services: Map<String, Service>,
    private val service: Service,
) {
    fun validate(
        schemaElement: NadelServiceSchemaElement,
    ): List<NadelSchemaValidationError> {
        return if (schemaElement.overall is GraphQLInputObjectType && schemaElement.underlying is GraphQLInputObjectType) {
            validate(
                parent = schemaElement,
                overallFields = schemaElement.overall.fields,
                underlyingFields = schemaElement.underlying.fields,
            )
        } else {
            emptyList()
        }
    }

    private fun validate(
        parent: NadelServiceSchemaElement,
        overallFields: List<GraphQLInputObjectField>,
        underlyingFields: List<GraphQLInputObjectField>,
    ): List<NadelSchemaValidationError> {
        val underlyingFieldsByName = underlyingFields.strictAssociateBy { it.name }

        return overallFields.mapNotNull { overallField ->
            val underlyingField = underlyingFieldsByName[overallField.name]

            if (underlyingField == null) {
                MissingUnderlyingInputField(service, parent, overallField)
            } else {
                // TODO: type check here
                null
            }
        }
    }
}
