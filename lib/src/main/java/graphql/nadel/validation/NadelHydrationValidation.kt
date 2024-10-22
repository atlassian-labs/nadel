package graphql.nadel.validation

import graphql.GraphQLContext
import graphql.nadel.definition.hydration.NadelHydrationArgumentDefinition
import graphql.nadel.definition.hydration.NadelHydrationDefinition
import graphql.nadel.definition.hydration.getHydrationDefinitions
import graphql.nadel.definition.renamed.isRenamed
import graphql.nadel.definition.virtualType.isVirtualType
import graphql.nadel.engine.util.getFieldAt
import graphql.nadel.engine.util.getFieldsAlong
import graphql.nadel.engine.util.isList
import graphql.nadel.engine.util.isNonNull
import graphql.nadel.engine.util.partitionCount
import graphql.nadel.engine.util.singleOfTypeOrNull
import graphql.nadel.engine.util.startsWith
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.engine.util.unwrapNonNull
import graphql.nadel.validation.NadelSchemaValidationError.CannotRenameHydratedField
import graphql.nadel.validation.NadelSchemaValidationError.DuplicatedHydrationArgument
import graphql.nadel.validation.NadelSchemaValidationError.FieldWithPolymorphicHydrationMustReturnAUnion
import graphql.nadel.validation.NadelSchemaValidationError.HydrationFieldMustBeNullable
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationArgumentValueSource
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationBackingField
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationFieldValueSource
import graphql.nadel.validation.NadelSchemaValidationError.HydrationDoesNotSupplyRequiredBackingFieldArgument
import graphql.nadel.validation.NadelSchemaValidationError.MultipleSourceArgsInBatchHydration
import graphql.nadel.validation.NadelSchemaValidationError.NoSourceArgsInBatchHydration
import graphql.nadel.validation.NadelSchemaValidationError.NonExistentHydrationBackingFieldArgument
import graphql.schema.GraphQLDirectiveContainer
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLNamedOutputType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLType
import graphql.schema.GraphQLUnionType
import graphql.validation.ValidationUtil
import java.util.Locale

internal class NadelHydrationValidation(
    private val typeValidation: NadelTypeValidation,
    private val overallSchema: GraphQLSchema,
) {
    private val validationUtil = ValidationUtil()
    private val nadelHydrationConditionValidation = NadelHydrationConditionValidation()

    fun validate(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
    ): List<NadelSchemaValidationResult> {
        if (overallField.isRenamed()) {
            return listOf(
                CannotRenameHydratedField(parent, overallField),
            )
        }

        val hydrations = overallField.getHydrationDefinitions()
        if (hydrations.isEmpty()) {
            error("Don't invoke hydration validation if there is no hydration silly")
        }

        val conditionValidationError = nadelHydrationConditionValidation
            .validateConditionsOnAllHydrations(hydrations, parent, overallField)
        if (conditionValidationError != null) {
            return listOf(conditionValidationError)
        }

        val hasMoreThanOneHydration = hydrations.size > 1
        val errors = hydrations
            .flatMap { hydration ->
                validate(parent, overallField, hydration, hasMoreThanOneHydration)
            }

        val hydrationMismatchErrors = limitBatchHydrationMismatch(parent, overallField, hydrations)
        val indexHydrationErrors = limitUseOfIndexHydration(parent, overallField, hydrations)
        val sourceFieldErrors = limitSourceField(parent, overallField, hydrations)

        return errors + hydrationMismatchErrors + indexHydrationErrors + sourceFieldErrors
    }

    private fun validate(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        hydration: NadelHydrationDefinition,
        hasMoreThanOneHydration: Boolean,
    ): List<NadelSchemaValidationResult> {
        val backingField = overallSchema.queryType.getFieldAt(hydration.backingField)
            ?: return listOf(
                MissingHydrationBackingField(parent, overallField, hydration),
            )

        return getArgumentErrors(parent, overallField, hydration, backingField) +
            getOutputTypeErrors(parent, overallField, backingField, hasMoreThanOneHydration) +
            getObjectIdentifierErrors(parent, overallField, hydration)
    }

    private fun getObjectIdentifierErrors(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        hydration: NadelHydrationDefinition,
    ): List<NadelSchemaValidationResult> {
        // e.g. context.jiraComment
        val pathToSourceInputField = hydration.arguments
            .singleOfTypeOrNull<NadelHydrationArgumentDefinition.ObjectField>()
            ?.pathToField
            ?: return emptyList() // Ignore this, checked elsewhere

        // Find offending object identifiers and generate errors
        return (hydration.inputIdentifiedBy ?: return emptyList())
            .asSequence()
            .filterNot { identifier ->
                // e.g. context.jiraComment.id
                identifier.sourceId
                    .split(".")
                    .startsWith(pathToSourceInputField)
            }
            .map { offendingObjectIdentifier ->
                NadelSchemaValidationError.ObjectIdentifierMustFollowSourceInputField(
                    type = parent,
                    field = overallField,
                    pathToSourceInputField = pathToSourceInputField,
                    offendingObjectIdentifier = offendingObjectIdentifier,
                )
            }
            .toList()
    }

    private fun limitBatchHydrationMismatch(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        hydrations: List<NadelHydrationDefinition>,
    ): List<NadelSchemaValidationResult> {
        val (batched, notBatched) = hydrations.partitionCount(::isBatched)

        return if (batched > 0 && notBatched > 0) {
            listOf(
                NadelSchemaValidationError.HydrationsMismatch(parent, overallField),
            )
        } else {
            emptyList()
        }
    }

    private fun limitSourceField(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        hydrations: List<NadelHydrationDefinition>,
    ): List<NadelSchemaValidationResult> {
        if (hydrations.size > 1) {
            val pathsToSourceFields = hydrations
                .asSequence()
                .flatMap { hydration ->
                    hydration
                        .arguments
                        .asSequence()
                        .filterIsInstance<NadelHydrationArgumentDefinition.ObjectField>()
                        .map { it.pathToField }
                }
                .toList()

            val parentType = parent.underlying as GraphQLFieldsContainer
            val anyListSourceInputField = pathsToSourceFields
                .any { pathToField ->
                    parentType
                        .getFieldsAlong(pathToField)
                        .any { field ->
                            field.type.unwrapNonNull().isList
                        }
                }

            if (anyListSourceInputField) {
                val uniqueSourceFieldPaths = pathsToSourceFields
                    .toSet()

                if (uniqueSourceFieldPaths.size > 1) {
                    return listOf(
                        NadelSchemaValidationError.MultipleHydrationSourceInputFields(parent, overallField),
                    )
                }
            }
        }

        return emptyList()
    }

    private fun limitUseOfIndexHydration(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        hydrations: List<NadelHydrationDefinition>,
    ): List<NadelSchemaValidationResult> {
        // todo: or maybe just don't allow polymorphic index hydration
        val (indexCount, nonIndexCount) = hydrations.partitionCount { it.isIndexed }
        if (indexCount > 0 && nonIndexCount > 0) {
            return listOf(
                NadelSchemaValidationError.MixedIndexHydration(parent, overallField),
            )
        }

        return emptyList()
    }

    private fun isBatched(hydration: NadelHydrationDefinition): Boolean {
        val backingFieldDef = overallSchema.queryType.getFieldAt(hydration.backingField)
        return hydration.isBatched || /*deprecated*/ backingFieldDef?.type?.unwrapNonNull()?.isList == true
    }

    private fun getOutputTypeErrors(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        backingField: GraphQLFieldDefinition,
        hasMoreThanOneHydration: Boolean,
    ): List<NadelSchemaValidationResult> {
        // Ensures that the underlying type of the backing field matches with the expected overall output type
        val overallType = overallField.type.unwrapAll()

        if ((overallType as? GraphQLDirectiveContainer)?.isVirtualType() == true) {
            return emptyList() // Bypass validation for now
        }

        // Polymorphic hydration must have union output
        if (hasMoreThanOneHydration && overallType !is GraphQLUnionType) {
            return listOf(FieldWithPolymorphicHydrationMustReturnAUnion(parent, overallField))
        }

        val acceptableOutputTypes: List<GraphQLNamedOutputType> = when (overallType) {
            is GraphQLUnionType -> overallType.types + overallType
            is GraphQLInterfaceType -> overallSchema.getImplementations(overallType) + overallType
            else -> listOf(overallType as GraphQLNamedOutputType)
        }

        val backingOutputTypes: List<GraphQLNamedOutputType> =
            when (val backingOutputType = backingField.type.unwrapAll()) {
                is GraphQLUnionType -> backingOutputType.types
                is GraphQLInterfaceType -> overallSchema.getImplementations(backingOutputType)
                else -> listOf(backingOutputType as GraphQLNamedOutputType)
            }

        val typeValidation = backingOutputTypes
            // Find incompatible output types
            .filter { backingOutputType ->
                acceptableOutputTypes.none { acceptableOutputType ->
                    typeValidation.isAssignableTo(lhs = acceptableOutputType, rhs = backingOutputType)
                }
            }
            .map { backingOutputType ->
                NadelSchemaValidationError.HydrationIncompatibleOutputType(
                    parentType = parent,
                    overallField = overallField,
                    backingField = backingField,
                    incompatibleOutputType = backingOutputType,
                )
            }

        // Hydrations can error out so they MUST always be nullable
        val outputTypeMustBeNullable = if (overallField.type.isNonNull) {
            listOf(
                HydrationFieldMustBeNullable(parent, overallField)
            )
        } else {
            emptyList()
        }

        return typeValidation + outputTypeMustBeNullable
    }

    private fun getArgumentErrors(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        hydration: NadelHydrationDefinition,
        backingField: GraphQLFieldDefinition,
    ): List<NadelSchemaValidationResult> {
        // Can only provide one value for an argument
        val duplicatedArgumentsErrors = getDuplicatedArgumentErrors(parent, overallField, hydration)
        if (duplicatedArgumentsErrors.isNotEmpty()) {
            return duplicatedArgumentsErrors
        }

        val missingBackingArgErrors = getMissingBackingArgumentErrors(backingField, hydration, parent, overallField)
        if (missingBackingArgErrors.isNotEmpty()) {
            return missingBackingArgErrors
        }

        val batchArgumentErrors = getBatchHydrationArgumentErrors(parent, overallField, hydration, backingField)
        if (batchArgumentErrors.isNotEmpty()) {
            return batchArgumentErrors
        }

        return hydration.arguments.flatMap { hydrationArgument ->
            val backingFieldArgument = backingField.getArgument(hydrationArgument.name)

            if (backingFieldArgument == null) {
                listOf(
                    NonExistentHydrationBackingFieldArgument(
                        parent,
                        overallField,
                        hydration,
                        argument = hydrationArgument.name,
                    )
                )
            } else {
                getHydrationArgumentErrors(
                    parent = parent,
                    overallField = overallField,
                    backingField = backingField,
                    hydration = hydration,
                    hydrationArgument = hydrationArgument,
                )
            }
        }
    }

    private fun getMissingBackingArgumentErrors(
        backingField: GraphQLFieldDefinition,
        hydration: NadelHydrationDefinition,
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
    ): List<HydrationDoesNotSupplyRequiredBackingFieldArgument> {
        return backingField.arguments
            .filter { it.type.isNonNull }
            .mapNotNull { backingArg ->
                val hydrationArg = hydration.arguments.find { it.name == backingArg.name }
                if (hydrationArg == null) {
                    HydrationDoesNotSupplyRequiredBackingFieldArgument(
                        parent,
                        overallField,
                        hydration,
                        argument = backingArg.name,
                    )
                } else {
                    null
                }
            }
    }

    private fun getBatchHydrationArgumentErrors(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        hydration: NadelHydrationDefinition,
        backingField: GraphQLFieldDefinition,
    ): List<NadelSchemaValidationError> {
        val isBatchHydration = backingField.type.unwrapNonNull().isList
        if (!isBatchHydration) {
            return emptyList()
        }

        val numberOfSourceArgs = hydration.arguments
            .count { it is NadelHydrationArgumentDefinition.ObjectField }

        return if (numberOfSourceArgs == 0) {
            listOf(NoSourceArgsInBatchHydration(parent, overallField))
        } else if (numberOfSourceArgs > 1) {
            listOf(MultipleSourceArgsInBatchHydration(parent, overallField))
        } else {
            emptyList()
        }
    }

    private fun getDuplicatedArgumentErrors(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        hydration: NadelHydrationDefinition,
    ): List<DuplicatedHydrationArgument> {
        return hydration.arguments
            .groupBy { it.name }
            .asSequence()
            .filter { (_, arguments) ->
                arguments.size > 1
            }
            .map { (_, arguments) ->
                DuplicatedHydrationArgument(parent, overallField, arguments)
            }
            .toList()
    }

    private fun getHydrationArgumentErrors(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        backingField: GraphQLFieldDefinition,
        hydration: NadelHydrationDefinition,
        hydrationArgument: NadelHydrationArgumentDefinition,
    ): List<NadelSchemaValidationResult> {
        val isBatchHydration = backingField.type.unwrapNonNull().isList

        return when (hydrationArgument) {
            is NadelHydrationArgumentDefinition.ObjectField -> getObjectFieldArgumentErrors(
                parent,
                overallField,
                backingField,
                hydration,
                hydrationArgument,
                isBatchHydration
            )
            is NadelHydrationArgumentDefinition.FieldArgument -> getFieldArgumentErrors(
                parent,
                overallField,
                backingField,
                hydration,
                hydrationArgument,
                isBatchHydration,
            )
            is NadelHydrationArgumentDefinition.StaticArgument -> getStaticArgumentErrors(
                parent,
                overallField,
                backingField,
                hydration,
                hydrationArgument,
            )
        }
    }

    private fun getObjectFieldArgumentErrors(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        backingField: GraphQLFieldDefinition,
        hydrationDefinition: NadelHydrationDefinition,
        hydrationArgumentDefinition: NadelHydrationArgumentDefinition.ObjectField,
        isBatchHydration: Boolean,
    ): List<NadelSchemaValidationError> {
        val sourceField = (parent.underlying as GraphQLFieldsContainer)
            .getFieldAt(hydrationArgumentDefinition.pathToField)

        return if (sourceField == null) {
            listOf(
                MissingHydrationFieldValueSource(parent, overallField, hydrationArgumentDefinition)
            )
        } else {
            listOfNotNull(
                validateSuppliedArgumentType(
                    isBatchHydration = isBatchHydration,
                    parent = parent,
                    overallField = overallField,
                    backingField = backingField,
                    hydrationDefinition = hydrationDefinition,
                    hydrationArgumentDefinition = hydrationArgumentDefinition,
                    suppliedType = sourceField.type,
                ),
                nadelHydrationConditionValidation.validateHydrationCondition(
                    parent,
                    overallField,
                    hydrationDefinition
                ),
            )
        }
    }

    private fun getFieldArgumentErrors(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        backingField: GraphQLFieldDefinition,
        hydrationDefinition: NadelHydrationDefinition,
        hydrationArgumentDefinition: NadelHydrationArgumentDefinition.FieldArgument,
        isBatchHydration: Boolean,
    ): List<NadelSchemaValidationError> {
        val argument = overallField.getArgument(hydrationArgumentDefinition.argumentName)

        return if (argument == null) {
            listOf(
                MissingHydrationArgumentValueSource(
                    parentType = parent,
                    overallField = overallField,
                    remoteArgSource = hydrationArgumentDefinition,
                ),
            )
        } else {
            listOfNotNull(
                validateSuppliedArgumentType(
                    isBatchHydration = isBatchHydration,
                    parent = parent,
                    overallField = overallField,
                    backingField = backingField,
                    hydrationDefinition = hydrationDefinition,
                    hydrationArgumentDefinition = hydrationArgumentDefinition,
                    suppliedType = argument.type,
                )
            )
        }
    }

    private fun validateSuppliedArgumentType(
        isBatchHydration: Boolean,
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        backingField: GraphQLFieldDefinition,
        hydrationDefinition: NadelHydrationDefinition,
        hydrationArgumentDefinition: NadelHydrationArgumentDefinition,
        suppliedType: GraphQLType,
    ): NadelSchemaValidationError? {
        val backingFieldArg = backingField.getArgument(hydrationArgumentDefinition.name)

        NadelHydrationArgumentTypeValidation()
            .isAssignable(
                isBatchHydration,
                suppliedType = suppliedType,
                requiredType = backingFieldArg.type,
            )
            .onError {
                return when (it) {
                    is NadelHydrationArgumentTypeValidationResult.IncompatibleField -> {
                        NadelSchemaValidationError.IncompatibleFieldInHydratedInputObject(
                            parentType = parent,
                            overallField = overallField,
                            hydration = hydrationDefinition,
                            hydrationArgument = hydrationArgumentDefinition,
                            suppliedFieldContainer = it.suppliedFieldContainer,
                            suppliedField = it.suppliedField,
                            requiredFieldContainer = it.requiredFieldContainer,
                            requiredField = it.requiredField,
                        )
                    }
                    is NadelHydrationArgumentTypeValidationResult.IncompatibleInputType -> {
                        NadelSchemaValidationError.IncompatibleHydrationArgumentType(
                            parentType = parent,
                            overallField = overallField,
                            hydration = hydrationDefinition,
                            hydrationArgument = hydrationArgumentDefinition,
                            suppliedType = it.suppliedType,
                            requiredType = it.requiredType,
                        )
                    }
                    is NadelHydrationArgumentTypeValidationResult.MissingInputField -> {
                        NadelSchemaValidationError.HydrationArgumentDoesNotSupplyRequiredInputObjectField(
                            parentType = parent,
                            overallField = overallField,
                            hydration = hydrationDefinition,
                            hydrationArgument = hydrationArgumentDefinition,
                            suppliedFieldContainer = it.suppliedFieldContainer,
                            requiredFieldContainer = it.requiredFieldContainer,
                            requiredField = it.requiredField,
                        )
                    }
                }
            }

        return null
    }

    private fun getStaticArgumentErrors(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        backingField: GraphQLFieldDefinition,
        hydrationDefinition: NadelHydrationDefinition,
        hydrationArgumentDefinition: NadelHydrationArgumentDefinition.StaticArgument,
    ): List<NadelSchemaValidationResult> {
        val staticArg = hydrationArgumentDefinition.staticValue
        val backingFieldArgument = backingField.getArgument(hydrationArgumentDefinition.name)

        return if (
            !validationUtil.isValidLiteralValue(
                staticArg,
                backingFieldArgument.type,
                overallSchema,
                GraphQLContext.getDefault(),
                Locale.getDefault()
            )
        ) {
            listOf(
                NadelSchemaValidationError.StaticArgIsNotAssignable(
                    parent,
                    overallField,
                    hydrationDefinition,
                    hydrationArgumentDefinition,
                    requiredArgumentType = backingFieldArgument.type,
                )
            )
        } else {
            emptyList()
        }
    }
}
