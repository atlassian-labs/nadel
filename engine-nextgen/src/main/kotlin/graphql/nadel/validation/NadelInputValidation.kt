package graphql.nadel.validation

import graphql.nadel.Service
import graphql.nadel.enginekt.util.strictAssociateBy
import graphql.nadel.validation.NadelSchemaValidationError.Companion.missingUnderlyingInputField
import graphql.schema.GraphQLInputObjectField
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLSchema

class NadelInputValidation(
    private val overallSchema: GraphQLSchema,
    services: Map<String, Service>,
    private val service: Service,
) {
    fun getIssues(
        schemaElement: NadelServiceSchemaElement,
    ): List<NadelSchemaValidationError> {
        return if (schemaElement.overall is GraphQLInputObjectType && schemaElement.underlying is GraphQLInputObjectType) {
            getIssues(
                parent = schemaElement,
                overallFields = schemaElement.overall.fields,
                underlyingFields = schemaElement.underlying.fields,
            )
        } else {
            emptyList()
        }
    }

    private fun getIssues(
        parent: NadelServiceSchemaElement,
        overallFields: List<GraphQLInputObjectField>,
        underlyingFields: List<GraphQLInputObjectField>,
    ): List<NadelSchemaValidationError> {
        val underlyingFieldsByName = underlyingFields.strictAssociateBy { it.name }

        return overallFields.mapNotNull { overallField ->
            val underlyingField = underlyingFieldsByName[overallField.name]

            if (underlyingField == null) {
                missingUnderlyingInputField(service, parent, overallField)
            } else {
                // TODO: type check here
                null
            }
        }
    }
}
