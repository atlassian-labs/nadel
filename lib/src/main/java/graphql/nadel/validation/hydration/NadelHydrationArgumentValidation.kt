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
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLType
import graphql.validation.ValidationUtil
import java.util.Locale

private val validationUtil = ValidationUtil()

private data class NadelHydrationArgumentValidationContext(
    val parent: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
    val hydrationDefinition: NadelHydrationDefinition,
    val backingField: GraphQLFieldDefinition,
    val isBatchHydration: Boolean,
)

internal class NadelHydrationArgumentValidation {
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
        validateDuplicateArguments().onErrorReturnInterim { return it }
        validateRequiredBackingArguments().onErrorReturnInterim { return it }

        if (isBatchHydration) {
            validateBatchHydrationArguments().onErrorReturnInterim { return it }
        }

        return hydrationDefinition.arguments
            .map { hydrationArgument ->
                validateArgument(hydrationArgument).onErrorCast { return it }
            }
            .asInterimSuccess()
    }

    context(NadelValidationContext, NadelHydrationArgumentValidationContext)
    private fun validateArgument(
        hydrationArgumentDefinition: NadelHydrationArgumentDefinition,
    ): NadelValidationInterimResult<NadelHydrationArgument> {
        val backingArgumentDefinition = backingField.getArgument(hydrationArgumentDefinition.backingFieldArgumentName)
            ?: return NadelHydrationReferencesNonExistentBackingArgumentError(
                parentType = parent,
                virtualField = virtualField,
                hydration = hydrationDefinition,
                argument = hydrationArgumentDefinition.backingFieldArgumentName,
            ).asInterimError()

        return when (hydrationArgumentDefinition) {
            is NadelHydrationArgumentDefinition.SourceField -> getObjectFieldArgument(
                hydrationArgumentDefinition = hydrationArgumentDefinition,
                backingArgumentDefinition = backingArgumentDefinition,
            )
            is NadelHydrationArgumentDefinition.VirtualFieldArgument -> getFieldArgument(
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
                backingArg.type.isNonNull && hydrationDefinition.arguments.none { it.backingFieldArgumentName == backingArg.name }
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
            .count { it is NadelHydrationArgumentDefinition.SourceField }

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
            .groupBy { it.backingFieldArgumentName }
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
        hydrationArgumentDefinition: NadelHydrationArgumentDefinition.SourceField,
        backingArgumentDefinition: GraphQLArgument,
    ): NadelValidationInterimResult<NadelHydrationArgument> {
        val sourceField = (parent.underlying as GraphQLFieldsContainer)
            .getFieldAt(hydrationArgumentDefinition.pathToSourceField)

        if (sourceField == null) {
            return NadelHydrationArgumentReferencesNonExistentFieldError(
                parentType = parent,
                virtualField = virtualField,
                hydration = hydrationDefinition,
                argument = hydrationArgumentDefinition
            ).asInterimError()
        }

        validateSuppliedArgumentValueType(
            hydrationArgumentDefinition = hydrationArgumentDefinition,
            suppliedType = sourceField.type,
        ).onErrorReturnInterim { return it }

        return NadelHydrationArgument.SourceField(
            backingArgumentDef = backingArgumentDefinition,
            sourceFieldDef = sourceField,
            pathToSourceField = NadelQueryPath(hydrationArgumentDefinition.pathToSourceField),
        ).asInterimSuccess()
    }

    context(NadelValidationContext, NadelHydrationArgumentValidationContext)
    private fun getFieldArgument(
        hydrationArgumentDefinition: NadelHydrationArgumentDefinition.VirtualFieldArgument,
        backingArgumentDefinition: GraphQLArgument,
    ): NadelValidationInterimResult<NadelHydrationArgument> {
        val virtualArgumentDefinition = virtualField.getArgument(hydrationArgumentDefinition.virtualFieldArgumentName)
            ?: return NadelHydrationArgumentReferencesNonExistentArgumentError(
                parentType = parent,
                virtualField = virtualField,
                hydration = hydrationDefinition,
                argument = hydrationArgumentDefinition,
            ).asInterimError()

        validateSuppliedArgumentValueType(
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

        return NadelHydrationArgument.VirtualFieldArgument(
            backingArgumentDef = backingArgumentDefinition,
            virtualFieldArgumentDef = virtualArgumentDefinition,
            defaultValue = defaultValue,
        ).asInterimSuccess()
    }

    context(NadelValidationContext, NadelHydrationArgumentValidationContext)
    private fun getStaticArgument(
        hydrationArgumentDefinition: NadelHydrationArgumentDefinition.StaticArgument,
        backingArgumentDefinition: GraphQLArgument,
    ): NadelValidationInterimResult<NadelHydrationArgument> {
        val staticArgValue = hydrationArgumentDefinition.staticValue
        val backingFieldArgument = backingField.getArgument(hydrationArgumentDefinition.backingFieldArgumentName)

        val isAssignable = validationUtil.isValidLiteralValue(
            staticArgValue,
            backingFieldArgument.type,
            engineSchema,
            GraphQLContext.getDefault(),
            Locale.getDefault(),
        )

        return if (isAssignable) {
            NadelHydrationArgument.StaticValue(
                backingArgumentDef = backingArgumentDefinition,
                normalizedInputValue = makeNormalizedInputValue(
                    type = backingFieldArgument.type,
                    value = hydrationArgumentDefinition.staticValue,
                ),
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
    private fun validateSuppliedArgumentValueType(
        hydrationArgumentDefinition: NadelHydrationArgumentDefinition,
        suppliedType: GraphQLType,
    ): NadelSchemaValidationResult {
        val backingFieldArg = backingField.getArgument(hydrationArgumentDefinition.backingFieldArgumentName)

        NadelHydrationArgumentTypeValidation()
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
