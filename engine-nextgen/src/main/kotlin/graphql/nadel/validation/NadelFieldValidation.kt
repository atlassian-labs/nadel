package graphql.nadel.validation

import graphql.nadel.Service
import graphql.nadel.enginekt.util.strictAssociateBy
import graphql.nadel.enginekt.util.unwrapAll
import graphql.nadel.validation.NadelSchemaValidationError.MissingArgumentOnUnderlying
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingField
import graphql.nadel.validation.util.NadelCombinedTypeUtil.getFieldsThatServiceContributed
import graphql.nadel.validation.util.NadelCombinedTypeUtil.isCombinedType
import graphql.nadel.validation.util.NadelSchemaUtil.hasHydration
import graphql.nadel.validation.util.NadelSchemaUtil.hasRename
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLNamedSchemaElement
import graphql.schema.GraphQLSchema

internal class NadelFieldValidation(
    private val overallSchema: GraphQLSchema,
    services: Map<String, Service>,
    private val typeValidation: NadelTypeValidation,
) {
    private val renameValidation = NadelRenameValidation(this)
    private val hydrationValidation = NadelHydrationValidation(services, typeValidation, overallSchema)

    fun validate(
        schemaElement: NadelServiceSchemaElement,
    ): List<NadelSchemaValidationError> {
        return if (schemaElement.overall is GraphQLFieldsContainer && schemaElement.underlying is GraphQLFieldsContainer) {
            validate(
                schemaElement,
                overallFields = schemaElement.overall.fields,
                underlyingFields = schemaElement.underlying.fields,
            )
        } else {
            emptyList()
        }
    }

    fun validate(
        parent: NadelServiceSchemaElement,
        overallFields: List<GraphQLFieldDefinition>,
        underlyingFields: List<GraphQLFieldDefinition>,
    ): List<NadelSchemaValidationError> {
        val underlyingFieldsByName = underlyingFields.strictAssociateBy { it.name }

        return overallFields
            .asSequence()
            .let { fieldSequence ->
                // Apply filter if necessary
                if (isCombinedType(type = parent.overall)) {
                    val fieldsThatServiceContributed = getFieldsThatServiceContributed(parent)
                    fieldSequence.filter { it.name in fieldsThatServiceContributed }
                } else {
                    fieldSequence
                }
            }
            .flatMap { overallField ->
                validate(parent, overallField, underlyingFieldsByName)
            }
            .toList()
    }

    fun validate(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        underlyingFieldsByName: Map<String, GraphQLFieldDefinition>,
    ): List<NadelSchemaValidationError> {
        return if (hasRename(overallField)) {
            renameValidation.validate(parent, overallField)
        } else if (hasHydration(overallField)) {
            hydrationValidation.validate(parent, overallField)
        } else {
            val underlyingField = underlyingFieldsByName[overallField.name]
            if (underlyingField == null) {
                listOf(
                    MissingUnderlyingField(parent, overallField = overallField),
                )
            } else {
                validate(
                    parent,
                    overallField = overallField,
                    underlyingField = underlyingField,
                )
            }
        }
    }

    fun validate(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        underlyingField: GraphQLFieldDefinition,
    ): List<NadelSchemaValidationError> {
        val argumentIssues = overallField.arguments.flatMap { overallArg ->
            val underlyingArg = underlyingField.getArgument(overallArg.name)
            if (underlyingArg == null) {
                listOf(
                    MissingArgumentOnUnderlying(parent, overallField, underlyingField, overallArg),
                )
            } else {
                // TODO check the type wrappings are equal
                typeValidation.validate(
                    NadelServiceSchemaElement(
                        service = parent.service,
                        overall = overallArg.type.unwrapAll(),
                        underlying = underlyingArg.type.unwrapAll(),
                    )
                )
            }
        }

        val outputTypeIssues = typeValidation.validateOutputType(parent, overallField, underlyingField)

        return argumentIssues + outputTypeIssues
    }

    private fun isCombinedType(type: GraphQLNamedSchemaElement): Boolean {
        return isCombinedType(overallSchema, type)
    }
}
