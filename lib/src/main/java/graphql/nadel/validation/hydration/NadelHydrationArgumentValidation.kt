package graphql.nadel.validation.hydration

import graphql.GraphQLContext
import graphql.language.Value
import graphql.nadel.definition.hydration.NadelHydrationArgumentDefinition
import graphql.nadel.definition.hydration.NadelHydrationDefinition
import graphql.nadel.engine.blueprint.hydration.NadelHydrationArgument
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.util.getFieldAt
import graphql.nadel.engine.util.isNonNull
import graphql.nadel.engine.util.makeNormalizedInputValue
import graphql.nadel.schema.NadelDirectives
import graphql.nadel.validation.NadelBatchHydrationArgumentInvalidSourceInputError
import graphql.nadel.validation.NadelBatchHydrationArgumentMissingSourceFieldError
import graphql.nadel.validation.NadelBatchHydrationArgumentMultipleSourceFieldsError
import graphql.nadel.validation.NadelHydrationArgumentDuplicatedError
import graphql.nadel.validation.NadelHydrationArgumentIncompatibleTypeError
import graphql.nadel.validation.NadelHydrationArgumentMissingRequiredInputObjectFieldError
import graphql.nadel.validation.NadelHydrationArgumentReferencesNonExistentArgumentError
import graphql.nadel.validation.NadelHydrationArgumentReferencesNonExistentFieldError
import graphql.nadel.validation.NadelHydrationIncompatibleInputObjectFieldError
import graphql.nadel.validation.NadelHydrationMissingRequiredBackingFieldArgumentError
import graphql.nadel.validation.NadelHydrationReferencesNonExistentBackingArgumentError
import graphql.nadel.validation.NadelSchemaValidationError
import graphql.nadel.validation.NadelSchemaValidationResult
import graphql.nadel.validation.NadelServiceSchemaElement
import graphql.nadel.validation.NadelValidationContext
import graphql.nadel.validation.NadelValidationInterimResult
import graphql.nadel.validation.NadelValidationInterimResult.Error.Companion.asInterimError
import graphql.nadel.validation.NadelValidationInterimResult.Success.Companion.asInterimSuccess
import graphql.nadel.validation.ok
import graphql.nadel.validation.onErrorCast
import graphql.nadel.validation.onErrorReturnInterim
import graphql.nadel.validation.toResult
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLType
import graphql.validation.ValidationUtil
import java.util.Locale

private val validationUtil = ValidationUtil()

private data class NadelHydrationArgumentValidationContext(
    val parent: NadelServiceSchemaElement.FieldsContainer,
    val virtualField: GraphQLFieldDefinition,
    val hydrationDefinition: NadelHydrationDefinition,
    val backingField: GraphQLFieldDefinition,
    val isBatchHydration: Boolean,
)

internal class NadelHydrationArgumentValidation(
    private val hydrationArgumentTypeValidation: NadelHydrationArgumentTypeValidation,
) {
    context(NadelValidationContext, NadelHydrationValidationContext)
    fun validateArguments(
        isBatchHydration: Boolean,
    ): NadelValidationInterimResult<List<NadelHydrationArgument>> {
        val argumentValidationContext = NadelHydrationArgumentValidationContext(
            parent = parent,
            virtualField = virtualField,
            hydrationDefinition = hydrationDefinition,
            backingField = backingField,
            isBatchHydration = isBatchHydration,
        )

        with(argumentValidationContext) {
            return validateArguments()
        }
    }

    context(NadelValidationContext, NadelHydrationArgumentValidationContext)
    private fun validateArguments(): NadelValidationInterimResult<List<NadelHydrationArgument>> {
        validateDuplicateArguments()
            .onErrorReturnInterim { return it }
        validateRequiredBackingArguments()
            .onErrorReturnInterim { return it }

        if (isBatchHydration) {
            validateBatchHydrationArguments()
                .onErrorReturnInterim { return it }
        }

        val arguments = hydrationDefinition.arguments
            .map { hydrationArgument ->
                validateArgument(hydrationArgument)
                    .onErrorCast { return it }
            }

        val remainingArguments = validateRemainingArguments(arguments)
            .onErrorCast { return it }

        return (arguments + listOfNotNull(remainingArguments)).asInterimSuccess()
    }

    context(NadelValidationContext, NadelHydrationArgumentValidationContext)
    private fun validateRemainingArguments(
        arguments: List<NadelHydrationArgument>,
    ): NadelValidationInterimResult<NadelHydrationArgument?> {
        val remainingArgument = backingField.arguments
            .firstOrNull {
                it.hasAppliedDirective(NadelDirectives.nadelHydrationRemainingArguments.name)
            }
            ?: return null.asInterimSuccess()

        return NadelHydrationArgument(
            name = remainingArgument.name,
            backingArgumentDef = remainingArgument,
            valueSource = NadelHydrationArgument.ValueSource.RemainingArguments(
                remainingArgumentNames = @Suppress("ConvertArgumentToSet") // Useless
                (virtualField.arguments.map { it.name } - arguments.map { it.name })
            ),
        ).asInterimSuccess()
    }

    context(NadelValidationContext, NadelHydrationArgumentValidationContext)
    private fun validateArgument(
        hydrationArgumentDefinition: NadelHydrationArgumentDefinition,
    ): NadelValidationInterimResult<NadelHydrationArgument> {
        val backingArgumentDefinition = backingField.getArgument(hydrationArgumentDefinition.name)
            ?: return NadelHydrationReferencesNonExistentBackingArgumentError(
                parentType = parent,
                virtualField = virtualField,
                hydration = hydrationDefinition,
                argument = hydrationArgumentDefinition.name,
            ).asInterimError()

        return when (hydrationArgumentDefinition) {
            is NadelHydrationArgumentDefinition.ObjectField -> getObjectFieldArgument(
                hydrationArgumentDefinition = hydrationArgumentDefinition,
                backingArgumentDefinition = backingArgumentDefinition,
            )
            is NadelHydrationArgumentDefinition.FieldArgument -> getFieldArgument(
                hydrationArgumentDefinition = hydrationArgumentDefinition,
                backingArgumentDefinition = backingArgumentDefinition,
            )
            is NadelHydrationArgumentDefinition.StaticArgument -> getStaticArgument(
                hydrationArgumentDefinition = hydrationArgumentDefinition,
                backingArgumentDefinition = backingArgumentDefinition,
            )
        }
    }

    context(NadelValidationContext, NadelHydrationArgumentValidationContext)
    private fun validateRequiredBackingArguments(): NadelSchemaValidationResult {
        return backingField.arguments
            .asSequence()
            .filter { backingArg ->
                backingArg.type.isNonNull && hydrationDefinition.arguments.none { it.name == backingArg.name }
            }
            .map { backingArg ->
                NadelHydrationMissingRequiredBackingFieldArgumentError(
                    parentType = parent,
                    virtualField = virtualField,
                    hydration = hydrationDefinition,
                    missingBackingArgument = backingArg,
                )
            }
            .toResult()
    }

    context(NadelValidationContext, NadelHydrationArgumentValidationContext)
    private fun validateBatchHydrationArguments(): NadelSchemaValidationResult {
        val numberOfSourceArgs = hydrationDefinition.arguments
            .count { it is NadelHydrationArgumentDefinition.ObjectField }

        return if (numberOfSourceArgs == 0) {
            NadelBatchHydrationArgumentMissingSourceFieldError(parent, virtualField, hydrationDefinition)
        } else if (numberOfSourceArgs > 1) {
            NadelBatchHydrationArgumentMultipleSourceFieldsError(parent, virtualField, hydrationDefinition)
        } else {
            ok()
        }
    }

    context(NadelValidationContext, NadelHydrationArgumentValidationContext)
    private fun validateDuplicateArguments(): NadelSchemaValidationResult {
        return hydrationDefinition.arguments
            .groupBy { it.name }
            .asSequence()
            .filter { (_, arguments) ->
                arguments.size > 1
            }
            .map { (_, arguments) ->
                NadelHydrationArgumentDuplicatedError(parent, virtualField, hydrationDefinition, arguments)
            }
            .toResult()
    }

    context(NadelValidationContext, NadelHydrationArgumentValidationContext)
    private fun getObjectFieldArgument(
        hydrationArgumentDefinition: NadelHydrationArgumentDefinition.ObjectField,
        backingArgumentDefinition: GraphQLArgument,
    ): NadelValidationInterimResult<NadelHydrationArgument> {
        val sourceField = parent.underlying
            .getFieldAt(hydrationArgumentDefinition.pathToField)

        if (sourceField == null) {
            return NadelHydrationArgumentReferencesNonExistentFieldError(
                parentType = parent,
                virtualField = virtualField,
                hydration = hydrationDefinition,
                argument = hydrationArgumentDefinition
            ).asInterimError()
        }

        validateSuppliedArgumentType(
            hydrationArgumentDefinition = hydrationArgumentDefinition,
            suppliedType = sourceField.type,
        ).onErrorReturnInterim { return it }

        return NadelHydrationArgument(
            name = hydrationArgumentDefinition.name,
            backingArgumentDef = backingArgumentDefinition,
            valueSource = NadelHydrationArgument.ValueSource.FieldResultValue(
                fieldDefinition = sourceField,
                queryPathToField = NadelQueryPath(hydrationArgumentDefinition.pathToField),
            )
        ).asInterimSuccess()
    }

    context(NadelValidationContext, NadelHydrationArgumentValidationContext)
    private fun getFieldArgument(
        hydrationArgumentDefinition: NadelHydrationArgumentDefinition.FieldArgument,
        backingArgumentDefinition: GraphQLArgument,
    ): NadelValidationInterimResult<NadelHydrationArgument> {
        val virtualArgumentDefinition = virtualField.getArgument(hydrationArgumentDefinition.argumentName)
            ?: return NadelHydrationArgumentReferencesNonExistentArgumentError(
                parentType = parent,
                virtualField = virtualField,
                hydration = hydrationDefinition,
                argument = hydrationArgumentDefinition,
            ).asInterimError()

        validateSuppliedArgumentType(
            hydrationArgumentDefinition = hydrationArgumentDefinition,
            suppliedType = virtualArgumentDefinition.type,
        ).onErrorReturnInterim { return it }

        val defaultValue = if (virtualArgumentDefinition.argumentDefaultValue.isLiteral) {
            makeNormalizedInputValue(
                virtualArgumentDefinition.type,
                virtualArgumentDefinition.argumentDefaultValue.value as Value<*>,
            )
        } else {
            null
        }

        return NadelHydrationArgument(
            name = hydrationArgumentDefinition.name,
            backingArgumentDef = backingArgumentDefinition,
            valueSource = NadelHydrationArgument.ValueSource.ArgumentValue(
                argumentName = hydrationArgumentDefinition.argumentName,
                argumentDefinition = virtualArgumentDefinition,
                defaultValue = defaultValue,
            )
        ).asInterimSuccess()
    }

    context(NadelValidationContext, NadelHydrationArgumentValidationContext)
    private fun getStaticArgument(
        hydrationArgumentDefinition: NadelHydrationArgumentDefinition.StaticArgument,
        backingArgumentDefinition: GraphQLArgument,
    ): NadelValidationInterimResult<NadelHydrationArgument> {
        val staticArgValue = hydrationArgumentDefinition.staticValue
        val backingFieldArgument = backingField.getArgument(hydrationArgumentDefinition.name)

        val isAssignable = validationUtil.isValidLiteralValue(
            staticArgValue,
            backingFieldArgument.type,
            engineSchema,
            GraphQLContext.getDefault(),
            Locale.getDefault(),
        )

        return if (isAssignable) {
            NadelHydrationArgument(
                name = hydrationArgumentDefinition.name,
                backingArgumentDef = backingArgumentDefinition,
                valueSource = NadelHydrationArgument.ValueSource.StaticValue(
                    value = hydrationArgumentDefinition.staticValue,
                )
            ).asInterimSuccess()
        } else {
            NadelSchemaValidationError.StaticArgIsNotAssignable(
                parentType = parent,
                overallField = virtualField,
                hydration = hydrationDefinition,
                hydrationArgument = hydrationArgumentDefinition,
                requiredArgumentType = backingFieldArgument.type,
            ).asInterimError()
        }
    }

    context(NadelValidationContext, NadelHydrationArgumentValidationContext)
    private fun validateSuppliedArgumentType(
        hydrationArgumentDefinition: NadelHydrationArgumentDefinition,
        suppliedType: GraphQLType,
    ): NadelSchemaValidationResult {
        val backingFieldArg = backingField.getArgument(hydrationArgumentDefinition.name)

        hydrationArgumentTypeValidation
            .isAssignable(
                isBatchHydration = isBatchHydration,
                hydrationArgumentDefinition = hydrationArgumentDefinition,
                suppliedType = suppliedType,
                requiredType = backingFieldArg.type,
            )
            .onError {
                return when (it) {
                    is NadelHydrationArgumentTypeValidationResult.IncompatibleField -> {
                        NadelHydrationIncompatibleInputObjectFieldError(
                            parentType = parent,
                            virtualField = virtualField,
                            hydration = hydrationDefinition,
                            hydrationArgument = hydrationArgumentDefinition,
                            suppliedFieldContainer = it.suppliedFieldContainer,
                            suppliedField = it.suppliedField,
                            requiredFieldContainer = it.requiredFieldContainer,
                            requiredField = it.requiredField,
                        )
                    }
                    is NadelHydrationArgumentTypeValidationResult.IncompatibleInputType -> {
                        NadelHydrationArgumentIncompatibleTypeError(
                            parentType = parent,
                            virtualField = virtualField,
                            hydration = hydrationDefinition,
                            hydrationArgument = hydrationArgumentDefinition,
                            suppliedType = it.suppliedType,
                            requiredType = it.requiredType,
                        )
                    }
                    is NadelHydrationArgumentTypeValidationResult.MissingInputField -> {
                        NadelHydrationArgumentMissingRequiredInputObjectFieldError(
                            parentType = parent,
                            virtualField = virtualField,
                            hydration = hydrationDefinition,
                            hydrationArgument = hydrationArgumentDefinition,
                            suppliedFieldContainer = it.suppliedFieldContainer,
                            requiredFieldContainer = it.requiredFieldContainer,
                            requiredField = it.requiredField,
                        )
                    }
                    NadelHydrationArgumentTypeValidationResult.InvalidBackingFieldBatchIdArg -> {
                        NadelBatchHydrationArgumentInvalidSourceInputError(
                            parentType = parent,
                            virtualField = virtualField,
                            hydration = hydrationDefinition,
                            hydrationArgument = hydrationArgumentDefinition,
                        )
                    }
                }
            }

        return ok()
    }
}
