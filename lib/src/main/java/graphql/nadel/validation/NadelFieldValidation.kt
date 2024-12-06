package graphql.nadel.validation

import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.schema.NadelDirectives
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleArgumentInputType
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleFieldOutputType
import graphql.nadel.validation.NadelSchemaValidationError.MissingArgumentOnUnderlying
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingField
import graphql.nadel.validation.NadelTypeWrappingValidation.Rule.LHS_MUST_BE_LOOSER_OR_SAME
import graphql.nadel.validation.hydration.NadelHydrationValidation
import graphql.nadel.validation.util.NadelCombinedTypeUtil.getFieldsThatServiceContributed
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLNamedSchemaElement
import graphql.schema.GraphQLOutputType

class NadelFieldValidation internal constructor(
    private val hydrationValidation: NadelHydrationValidation,
) {
    private val renameValidation = NadelRenameValidation(this)
    private val inputValidation = NadelInputValidation()
    private val partitionValidation = NadelPartitionValidation()
    private val typeWrappingValidation = NadelTypeWrappingValidation()

    context(NadelValidationContext)
    fun validate(
        schemaElement: NadelServiceSchemaElement.FieldsContainer,
    ): NadelSchemaValidationResult {
        return validate(
            schemaElement,
            overallFields = schemaElement.overall.fields,
        )
    }

    context(NadelValidationContext)
    fun validate(
        parent: NadelServiceSchemaElement.FieldsContainer,
        overallFields: List<GraphQLFieldDefinition>,
    ): NadelSchemaValidationResult {
        if(areAllFieldsHidden(overallFields)) {
            return NadelSchemaValidationError.AllFieldsUsingHiddenDirective(parent)
        }
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
            .map { overallField ->
                validate(parent, overallField)
            }
            .toResult()
    }

    private fun areAllFieldsHidden(overallFields: List<GraphQLFieldDefinition>): Boolean {
        return overallFields.all { it.hasAppliedDirective(NadelDirectives.hiddenDirectiveDefinition.name) };
    }

    context(NadelValidationContext)
    fun validate(
        parent: NadelServiceSchemaElement.FieldsContainer,
        overallField: GraphQLFieldDefinition,
    ): NadelSchemaValidationResult {
        return if (isRenamed(parent, overallField)) {
            renameValidation.validate(parent, overallField)
        } else if (isHydrated(parent, overallField)) {
            hydrationValidation.validate(parent, overallField)
        } else {
            val underlyingField = parent.underlying.getField(overallField.name)
            if (underlyingField == null) {
                MissingUnderlyingField(parent, overallField = overallField)
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
        parent: NadelServiceSchemaElement.FieldsContainer,
        overallField: GraphQLFieldDefinition,
        underlyingField: GraphQLFieldDefinition,
    ): NadelSchemaValidationResult {
        val argumentIssues = overallField.arguments
            .map { overallArg ->
                val underlyingArg = underlyingField.getArgument(overallArg.name)
                if (underlyingArg == null) {
                    MissingArgumentOnUnderlying(parent, overallField, underlyingField, overallArg)
                } else {
                    if (isUnwrappedArgTypeSame(overallArg, underlyingArg)) {
                        inputValidation
                            .validate(
                                parent = parent,
                                overallField = overallField,
                                overallInputArgument = overallArg,
                                underlyingInputArgument = underlyingArg
                            )
                    } else {
                        IncompatibleArgumentInputType(
                            parentType = parent,
                            overallField = overallField,
                            overallInputArg = overallArg,
                            underlyingInputArg = underlyingArg,
                        )
                    }
                }
            }
            .toResult()

        val outputTypeIssues = validateOutputType(parent, overallField, underlyingField)
        val partitionDirectiveIssues = partitionValidation.validate(parent, overallField)

        return results(argumentIssues, outputTypeIssues, partitionDirectiveIssues)
    }

    context(NadelValidationContext)
    private fun isUnwrappedArgTypeSame(
        overallArg: GraphQLArgument,
        underlyingArg: GraphQLArgument,
    ): Boolean {
        val overallArgTypeUnwrapped = overallArg.type.unwrapAll()
        val underlyingArgTypeUnwrapped = underlyingArg.type.unwrapAll()
        return getUnderlyingTypeName(overallArgTypeUnwrapped) == underlyingArgTypeUnwrapped.name
    }

    context(NadelValidationContext)
    private fun validateOutputType(
        parent: NadelServiceSchemaElement.FieldsContainer,
        overallField: GraphQLFieldDefinition,
        underlyingField: GraphQLFieldDefinition,
    ): NadelSchemaValidationResult {
        // This checks whether the output type e.g. name or List or NonNull wrappings are valid
        return if (isOutputTypeValid(overallType = overallField.type, underlyingType = underlyingField.type)) {
            ok()
        } else {
            results(
                IncompatibleFieldOutputType(parent, overallField, underlyingField),
            )
        }
    }

    /**
     * It checks whether the type name and type wrappings e.g. [graphql.schema.GraphQLNonNull] make sense.
     */
    context(NadelValidationContext)
    private fun isOutputTypeValid(
        overallType: GraphQLOutputType,
        underlyingType: GraphQLOutputType,
    ): Boolean {
        val isTypeWrappingValid = typeWrappingValidation
            .isTypeWrappingValid(
                lhs = overallType,
                rhs = underlyingType,
                rule = LHS_MUST_BE_LOOSER_OR_SAME,
            )

        return isTypeWrappingValid
            && getUnderlyingTypeName(overallType.unwrapAll()) == underlyingType.unwrapAll().name
    }

    context(NadelValidationContext)
    private fun isCombinedType(type: GraphQLNamedSchemaElement): Boolean {
        return type.name in combinedTypeNames
    }
}
