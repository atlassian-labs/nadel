package graphql.nadel.validation

import graphql.nadel.definition.hydration.isHydrated
import graphql.nadel.definition.renamed.isRenamed
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.validation.NadelSchemaValidationError.MissingArgumentOnUnderlying
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingField
import graphql.nadel.validation.util.NadelCombinedTypeUtil.getFieldsThatServiceContributed
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLNamedSchemaElement

internal class NadelFieldValidation(
    private val typeValidation: NadelTypeValidation,
) {
    private val renameValidation = NadelRenameValidation(this)
    private val inputValidation = NadelInputValidation()
    private val hydrationValidation = NadelHydrationValidation(typeValidation)

    context(NadelValidationContext)
    fun validate(
        schemaElement: NadelServiceSchemaElement,
    ): List<NadelSchemaValidationResult> {
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

    context(NadelValidationContext)
    fun validate(
        parent: NadelServiceSchemaElement,
        overallFields: List<GraphQLFieldDefinition>,
        underlyingFields: List<GraphQLFieldDefinition>,
    ): List<NadelSchemaValidationResult> {
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

    context(NadelValidationContext)
    fun validate(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        underlyingFieldsByName: Map<String, GraphQLFieldDefinition>,
    ): List<NadelSchemaValidationResult> {
        return if (overallField.isRenamed()) {
            renameValidation.validate(parent, overallField)
        } else if (overallField.isHydrated()) {
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

    context(NadelValidationContext)
    fun validate(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        underlyingField: GraphQLFieldDefinition,
    ): List<NadelSchemaValidationResult> {
        overallField.arguments
            .forEach { overallArg ->
                val underlyingArg = underlyingField.getArgument(overallArg.name)
                if (underlyingArg == null) {
                    return listOf(
                        MissingArgumentOnUnderlying(parent, overallField, underlyingField, overallArg),
                    )
                } else {
                    typeValidation.validate(
                        NadelServiceSchemaElement(
                            service = parent.service,
                            overall = overallArg.type.unwrapAll(),
                            underlying = underlyingArg.type.unwrapAll(),
                        )
                    ).onError { return it }

                    inputValidation.validate(parent, overallField, overallArg, underlyingArg)
                        .onError { return it }
                }
            }

        typeValidation.validateOutputType(parent, overallField, underlyingField)
            .onError { return it }

        return emptyList()
    }

    context(NadelValidationContext)
    private fun isCombinedType(type: GraphQLNamedSchemaElement): Boolean {
        return type.name in combinedTypeNames
    }
}
