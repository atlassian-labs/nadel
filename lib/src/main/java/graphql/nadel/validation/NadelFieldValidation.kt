package graphql.nadel.validation

import graphql.language.DirectivesContainer
import graphql.nadel.definition.hydration.isHydrated
import graphql.nadel.definition.renamed.isRenamed
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.schema.NadelDirectives
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleFieldOutputType
import graphql.nadel.validation.NadelSchemaValidationError.MissingArgumentOnUnderlying
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingField
import graphql.nadel.validation.NadelTypeWrappingValidation.Rule.LHS_MUST_BE_LOOSER_OR_SAME
import graphql.nadel.validation.hydration.NadelHydrationValidation
import graphql.nadel.validation.util.NadelCombinedTypeUtil.getFieldsThatServiceContributed
import graphql.nadel.validation.util.NadelSchemaUtil.getUnderlyingName
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLNamedSchemaElement
import graphql.schema.GraphQLOutputType
import kotlin.streams.asStream

internal class NadelFieldValidation(
    private val typeValidation: NadelTypeValidation,
) {
    private val renameValidation = NadelRenameValidation(this)
    private val inputValidation = NadelInputValidation()
    private val hydrationValidation = NadelHydrationValidation(typeValidation)
    private val partitionValidation = NadelPartitionValidation()
    private val typeWrappingValidation = NadelTypeWrappingValidation()

    context(NadelValidationContext)
    fun validate(
        schemaElement: NadelServiceSchemaElement.FieldsContainer,
    ): NadelSchemaValidationResult {
        return validate(
            schemaElement,
            overallFields = schemaElement.overall.fields,
            underlyingFields = schemaElement.underlying.fields,
        )
    }

    fun DirectivesContainer<*>.isHidden(): Boolean {
        return hasDirective(NadelDirectives.hiddenDirectiveDefinition.name)
    }

    context(NadelValidationContext)
    fun validate(
        parent: NadelServiceSchemaElement.FieldsContainer,
        overallFields: List<GraphQLFieldDefinition>,
        underlyingFields: List<GraphQLFieldDefinition>,
    ): NadelSchemaValidationResult {
        val underlyingFieldsByName = underlyingFields.strictAssociateBy { it.name }
        //TODO : Validation
        var areAllFieldsHidden : Boolean

        return overallFields
            .asSequence()
            .let { fieldSequence ->
                areAllFieldsHidden = fieldSequence.all { it.hasAppliedDirective(NadelDirectives.hiddenDirectiveDefinition.name)}
                if (isCombinedType(type = parent.overall)) {
                    val fieldsThatServiceContributed = getFieldsThatServiceContributed(parent)
                    fieldSequence.filter { it.name in fieldsThatServiceContributed }
                } else {
                    fieldSequence
                }
            }
            .map { overallField ->
                validate(parent, overallField, underlyingFieldsByName, areAllFieldsHidden)
            }
            .toResult()
    }

    context(NadelValidationContext)
    fun validate(
        parent: NadelServiceSchemaElement.FieldsContainer,
        overallField: GraphQLFieldDefinition,
        underlyingFieldsByName: Map<String, GraphQLFieldDefinition>,
        areAllFieldsHidden : Boolean
    ): NadelSchemaValidationResult {
        return if(!areAllFieldsHidden) {
            NadelSchemaValidationError.AllFieldsUsingHiddenDirective(parent, overallField)
        }
        else if (overallField.isRenamed()) {
            renameValidation.validate(parent, overallField)
        } else if (overallField.isHydrated()) {
            hydrationValidation.validate(parent, overallField)
        } else {
            val underlyingField = underlyingFieldsByName[overallField.name]
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
                    val unwrappedTypeIssues = typeValidation
                        .validate(
                            NadelServiceSchemaElement.from(
                                service = parent.service,
                                overall = overallArg.type.unwrapAll(),
                                underlying = underlyingArg.type.unwrapAll(),
                            )
                        )

                    val inputTypeIssues = inputValidation
                        .validate(
                            parent = parent,
                            overallField = overallField,
                            overallInputArgument = overallArg,
                            underlyingInputArgument = underlyingArg
                        )

                    results(unwrappedTypeIssues, inputTypeIssues)
                }
            }
            .toResult()

        val outputTypeIssues = validateOutputType(parent, overallField, underlyingField)
        val partitionDirectiveIssues = partitionValidation.validate(parent, overallField)

        return results(argumentIssues, outputTypeIssues, partitionDirectiveIssues)
    }

    context(NadelValidationContext)
    private fun validateOutputType(
        parent: NadelServiceSchemaElement.FieldsContainer,
        overallField: GraphQLFieldDefinition,
        underlyingField: GraphQLFieldDefinition,
    ): NadelSchemaValidationResult {
        val overallType = overallField.type.unwrapAll()
        val underlyingType = underlyingField.type.unwrapAll()

        val typeServiceSchemaElement = NadelServiceSchemaElement.from(
            service = parent.service,
            overall = overallType,
            underlying = underlyingType,
        )

        // This checks whether the type is actually valid content wise
        val outputTypeResult = typeValidation.validate(typeServiceSchemaElement)
            .onError { return it }

        // This checks whether the output type e.g. name or List or NonNull wrappings are valid
        return if (isOutputTypeValid(overallType = overallField.type, underlyingType = underlyingField.type)) {
            outputTypeResult
        } else {
            results(
                outputTypeResult,
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
            && getUnderlyingName(overallType.unwrapAll()) == underlyingType.unwrapAll().name
    }

    context(NadelValidationContext)
    private fun isCombinedType(type: GraphQLNamedSchemaElement): Boolean {
        return type.name in combinedTypeNames
    }
}
