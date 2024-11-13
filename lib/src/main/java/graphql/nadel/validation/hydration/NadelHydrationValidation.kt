package graphql.nadel.validation.hydration

import graphql.nadel.Service
import graphql.nadel.definition.hydration.NadelBatchObjectIdentifiedByDefinition
import graphql.nadel.definition.hydration.NadelHydrationArgumentDefinition
import graphql.nadel.definition.hydration.NadelHydrationDefinition
import graphql.nadel.definition.hydration.getHydrationDefinitions
import graphql.nadel.definition.renamed.isRenamed
import graphql.nadel.definition.virtualType.isVirtualType
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.blueprint.hydration.NadelHydrationArgument
import graphql.nadel.engine.blueprint.hydration.NadelHydrationCondition
import graphql.nadel.engine.blueprint.hydration.NadelHydrationStrategy
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.util.emptyOrSingle
import graphql.nadel.engine.util.getFieldAt
import graphql.nadel.engine.util.getFieldContainerFor
import graphql.nadel.engine.util.getFieldsAlong
import graphql.nadel.engine.util.isList
import graphql.nadel.engine.util.isNonNull
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.engine.util.partitionCount
import graphql.nadel.engine.util.singleOfTypeOrNull
import graphql.nadel.engine.util.startsWith
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.engine.util.unwrapNonNull
import graphql.nadel.validation.NadelBatchHydrationArgumentMissingSourceFieldError
import graphql.nadel.validation.NadelBatchHydrationMatchingStrategyInvalidSourceIdError
import graphql.nadel.validation.NadelBatchHydrationMatchingStrategyReferencesNonExistentSourceFieldError
import graphql.nadel.validation.NadelBatchHydrationMissingIdentifiedByError
import graphql.nadel.validation.NadelHydrationCannotSqueezeSourceListError
import graphql.nadel.validation.NadelHydrationIncompatibleOutputTypeError
import graphql.nadel.validation.NadelHydrationMustUseIndexExclusivelyError
import graphql.nadel.validation.NadelHydrationReferencesNonExistentBackingFieldError
import graphql.nadel.validation.NadelHydrationTypeMismatchError
import graphql.nadel.validation.NadelHydrationVirtualFieldMustBeNullableError
import graphql.nadel.validation.NadelPolymorphicHydrationIncompatibleSourceFieldsError
import graphql.nadel.validation.NadelPolymorphicHydrationMustOutputUnionError
import graphql.nadel.validation.NadelSchemaValidationError.CannotRenameHydratedField
import graphql.nadel.validation.NadelSchemaValidationResult
import graphql.nadel.validation.NadelServiceSchemaElement
import graphql.nadel.validation.NadelTypeValidation
import graphql.nadel.validation.NadelValidatedFieldResult
import graphql.nadel.validation.NadelValidationContext
import graphql.nadel.validation.NadelValidationInterimResult
import graphql.nadel.validation.NadelValidationInterimResult.Error.Companion.asInterimError
import graphql.nadel.validation.NadelValidationInterimResult.Success.Companion.asInterimSuccess
import graphql.nadel.validation.ok
import graphql.nadel.validation.onError
import graphql.nadel.validation.onErrorCast
import graphql.nadel.validation.toResult
import graphql.schema.GraphQLDirectiveContainer
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLNamedOutputType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLUnionType

internal data class NadelHydrationValidationContext(
    val hasMoreThanOneHydration: Boolean,
    val parent: NadelServiceSchemaElement,
    val virtualField: GraphQLFieldDefinition,
    val hydrationDefinition: NadelHydrationDefinition,
    val isBatchHydration: Boolean,
    val backingFieldContainer: GraphQLFieldsContainer,
    val backingField: GraphQLFieldDefinition,
)

internal class NadelHydrationValidation(
    private val typeValidation: NadelTypeValidation,
    private val argumentValidation: NadelHydrationArgumentValidation = NadelHydrationArgumentValidation(),
    private val conditionValidation: NadelHydrationConditionValidation = NadelHydrationConditionValidation(),
    private val sourceFieldValidation: NadelHydrationSourceFieldValidation = NadelHydrationSourceFieldValidation(),
) {
    context(NadelValidationContext)
    fun validate(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
    ): NadelSchemaValidationResult {
        if (overallField.isRenamed()) {
            return CannotRenameHydratedField(parent, overallField)
        }

        val hydrations = overallField.getHydrationDefinitions()
        if (hydrations.isEmpty()) {
            error("Don't invoke hydration validation if there is no hydration silly")
        }

        conditionValidation
            .validateHydrations(hydrations, parent, overallField)
            .onError { return it }

        val hasMoreThanOneHydration = hydrations.size > 1

        limitBatchHydrationMismatch(parent, overallField, hydrations)
            .onError { return it }
        limitUseOfIndexHydration(parent, overallField, hydrations)
            .onError { return it }
        limitSourceField(parent, overallField, hydrations)
            .onError { return it }

        return hydrations
            .map { hydration ->
                validate(parent, overallField, hydration, hasMoreThanOneHydration)
            }
            .toResult()
    }

    context(NadelValidationContext)
    private fun validate(
        parent: NadelServiceSchemaElement,
        virtualField: GraphQLFieldDefinition,
        hydrationDefinition: NadelHydrationDefinition,
        hasMoreThanOneHydration: Boolean,
    ): NadelSchemaValidationResult {
        val backingFieldContainer = getBackingFieldContainer(parent, virtualField, hydrationDefinition)
            .onError { return it }

        val backingFieldDef = getBackingField(parent, virtualField, hydrationDefinition)
            .onError { return it }

        val isBatchHydration = isBatchHydration(hydrationDefinition)

        val context = NadelHydrationValidationContext(
            hasMoreThanOneHydration = hasMoreThanOneHydration,
            parent = parent,
            virtualField = virtualField,
            hydrationDefinition = hydrationDefinition,
            isBatchHydration = isBatchHydration,
            backingFieldContainer = backingFieldContainer,
            backingField = backingFieldDef,
        )

        with(context) {
            return validate()
        }
    }

    context(NadelValidationContext, NadelHydrationValidationContext)
    private fun validate(): NadelSchemaValidationResult {
        validateOutputType()
            .onError { return it }

        val arguments =
            argumentValidation.validateArguments(isBatchHydration)
                .onError { return it }

        val backingService =
            fieldContributor[makeFieldCoordinates(backingFieldContainer.name, backingField.name)]!!

        val hydrationCondition = hydrationDefinition.condition?.let { conditionDefinition ->
            conditionValidation.validateCondition(
                parent = parent,
                virtualField = virtualField,
                hydration = hydrationDefinition,
                condition = conditionDefinition,
            ).onError { return it }
        }

        // We only look at concrete instructions, tbh this should probably error out
        if (parent.overall !is GraphQLObjectType) {
            return ok()
        }

        return if (isBatchHydration) {
            validateBatchHydration(
                backingService = backingService,
                arguments = arguments,
                hydrationCondition = hydrationCondition,
            )
        } else {
            validateNonBatchHydration(
                backingService = backingService,
                arguments = arguments,
                hydrationCondition = hydrationCondition,
            )
        }
    }

    context(NadelValidationContext, NadelHydrationValidationContext)
    private fun validateNonBatchHydration(
        backingService: Service,
        arguments: List<NadelHydrationArgument>,
        hydrationCondition: NadelHydrationCondition?,
    ): NadelSchemaValidationResult {
        val hydrationStrategy = validateNonBatchHydrationStrategy(arguments)
            .onError { return it }
        val sourceFields = sourceFieldValidation.getSourceFields(arguments, hydrationCondition)
            .onError { return it }

        return NadelValidatedFieldResult(
            service = parent.service,
            fieldInstruction = NadelHydrationFieldInstruction(
                location = makeFieldCoordinates(parent.overall.name, virtualField.name),
                virtualFieldDef = virtualField,
                backingService = backingService,
                queryPathToBackingField = NadelQueryPath(hydrationDefinition.backingField),
                backingFieldArguments = arguments,
                timeout = hydrationDefinition.timeout,
                sourceFields = sourceFields,
                backingFieldDef = backingField,
                backingFieldContainer = backingFieldContainer,
                condition = hydrationCondition,
                virtualTypeContext = null,
                // todo: code properly
                hydrationStrategy = hydrationStrategy,
            ),
        )
    }

    context(NadelValidationContext, NadelHydrationValidationContext)
    private fun validateNonBatchHydrationStrategy(
        arguments: List<NadelHydrationArgument>,
    ): NadelValidationInterimResult<NadelHydrationStrategy> {
        val sourceFieldsTypeContainer = if ((parent.overall as GraphQLDirectiveContainer).isVirtualType()) {
            parent.overall
        } else {
            parent.underlying
        } as GraphQLObjectType

        val argumentDefToSplit = hydrationDefinition.arguments
            .asSequence()
            .filterIsInstance<NadelHydrationArgumentDefinition.SourceField>()
            .filter { hydrationArgument ->
                !backingField.getArgument(hydrationArgument.backingFieldArgumentName).type.unwrapNonNull().isList
                    && sourceFieldsTypeContainer
                    .getFieldsAlong(hydrationArgument.pathToSourceField)
                    .any { fieldDef ->
                        fieldDef.type.unwrapNonNull().isList
                    }
            }
            .emptyOrSingle()

        return if (argumentDefToSplit == null) {
            NadelHydrationStrategy.OneToOne
        } else {
            // I mean, in theory this could just be one to one?
            if (!virtualField.type.unwrapNonNull().isList) {
                NadelHydrationCannotSqueezeSourceListError(
                    parentType = parent,
                    virtualField = virtualField,
                    hydration = hydrationDefinition,
                    sourceField = argumentDefToSplit,
                )
            }

            NadelHydrationStrategy.ManyToOne(
                // Get the built argument, for the given argument definition…
                arguments
                    .asSequence()
                    .filterIsInstance<NadelHydrationArgument.SourceField>()
                    .single { it.name == argumentDefToSplit.backingFieldArgumentName },
            )
        }.asInterimSuccess()
    }

    context(NadelValidationContext, NadelHydrationValidationContext)
    private fun validateBatchHydration(
        backingService: Service,
        arguments: List<NadelHydrationArgument>,
        hydrationCondition: NadelHydrationCondition?,
    ): NadelSchemaValidationResult {
        val matchStrategy = validateBatchMatchingStrategy()
            .onError { return it }

        val sourceFields =
            sourceFieldValidation.getBatchHydrationSourceFields(arguments, matchStrategy, hydrationCondition)
                .onError { return it }

        return NadelValidatedFieldResult(
            service = parent.service,
            fieldInstruction = NadelBatchHydrationFieldInstruction(
                location = makeFieldCoordinates(parent.overall.name, virtualField.name),
                virtualFieldDef = virtualField,
                backingService = backingService,
                queryPathToBackingField = NadelQueryPath(hydrationDefinition.backingField),
                backingFieldArguments = arguments,
                timeout = hydrationDefinition.timeout,
                sourceFields = sourceFields,
                backingFieldDef = backingField,
                backingFieldContainer = backingFieldContainer,
                condition = hydrationCondition,
                batchSize = hydrationDefinition.batchSize,
                batchHydrationMatchStrategy = matchStrategy,
            )
        )
    }

    context(NadelValidationContext)
    private fun getBackingFieldContainer(
        parent: NadelServiceSchemaElement,
        virtualField: GraphQLFieldDefinition,
        hydration: NadelHydrationDefinition,
    ): NadelValidationInterimResult<GraphQLFieldsContainer> {
        return engineSchema.queryType.getFieldContainerFor(hydration.backingField)?.asInterimSuccess()
            ?: return NadelHydrationReferencesNonExistentBackingFieldError(
                parentType = parent,
                virtualField = virtualField,
                hydration = hydration
            ).asInterimError()
    }

    context(NadelValidationContext)
    private fun getBackingField(
        parent: NadelServiceSchemaElement,
        virtualField: GraphQLFieldDefinition,
        hydration: NadelHydrationDefinition,
    ): NadelValidationInterimResult<GraphQLFieldDefinition> {
        return engineSchema.queryType.getFieldAt(hydration.backingField)?.asInterimSuccess()
            ?: return NadelHydrationReferencesNonExistentBackingFieldError(
                parentType = parent,
                virtualField = virtualField,
                hydration = hydration
            ).asInterimError()
    }

    context(NadelValidationContext, NadelHydrationValidationContext)
    private fun validateBatchMatchingStrategy(): NadelValidationInterimResult<NadelBatchHydrationMatchStrategy> {
        if (hydrationDefinition.isIndexed) {
            return NadelBatchHydrationMatchStrategy.MatchIndex.asInterimSuccess()
        }

        val inputIdentifiedBy = hydrationDefinition.inputIdentifiedBy
        return if (inputIdentifiedBy?.isNotEmpty() == true) {
            validateInputIdentifiedByMatchingStrategy(
                parent,
                virtualField,
                hydrationDefinition,
                inputIdentifiedBy,
            )
        } else {
            val identifiedBy = getBatchIdentifiedBy()
                .onErrorCast { return it }

            NadelBatchHydrationMatchStrategy.MatchObjectIdentifier(
                sourceId = NadelQueryPath(
                    hydrationDefinition
                        .arguments
                        .asSequence()
                        .filterIsInstance<NadelHydrationArgumentDefinition.SourceField>()
                        .single()
                        .pathToSourceField,
                ),
                resultId = identifiedBy,
            ).asInterimSuccess()
        }
    }

    context(NadelValidationContext)
    private fun NadelHydrationValidationContext.getBatchIdentifiedBy(): NadelValidationInterimResult<String> {
        val identifiedBy = hydrationDefinition.identifiedBy
            ?: return NadelBatchHydrationMissingIdentifiedByError(
                parent,
                virtualField,
                hydrationDefinition,
            ).asInterimError()

        // val backingFieldContainerType = engineSchema.queryType.getFieldContainerFor(hydration.backingField)!!
        // val backingField = engineSchema.queryType.getFieldAt(hydration.backingField)!!
        // val backingService = fieldContributor[
        //     makeFieldCoordinates(backingFieldContainerType.name, backingField.name)
        // ]!!
        // val underlyingBackingFieldPath = engineSchema.queryType.getFieldsAlong(hydration.backingField)
        //     .map {
        //         it.getRenamedOrNull()?.from?.single() ?: it.name
        //     }
        // val underlyingBackingField =
        //     backingService.underlyingSchema.queryType.getFieldAt(underlyingBackingFieldPath)!!
        // val backingFieldOutputType = underlyingBackingField.type.unwrapAll()
        //
        // if (backingFieldOutputType is GraphQLUnionType) {
        //     backingFieldOutputType.types
        //         .onEach { objectType ->
        //             if ((objectType as GraphQLObjectType).getField(identifiedBy) == null) {
        //                 error("No result field found")
        //             }
        //         }
        // } else if (backingFieldOutputType is GraphQLFieldsContainer) {
        //     if (backingFieldOutputType.getField(identifiedBy) == null) {
        //         error("No result field found")
        //     }
        // } else {
        //     error("Unsupported output type")
        // }

        return identifiedBy.asInterimSuccess()
    }

    context(NadelValidationContext)
    private fun validateInputIdentifiedByMatchingStrategy(
        parent: NadelServiceSchemaElement,
        virtualField: GraphQLFieldDefinition,
        hydration: NadelHydrationDefinition,
        inputIdentifiedBy: List<NadelBatchObjectIdentifiedByDefinition>,
    ): NadelValidationInterimResult<NadelBatchHydrationMatchStrategy> {
        val pathToSourceInputField = hydration.arguments
            .singleOfTypeOrNull<NadelHydrationArgumentDefinition.SourceField>()
            ?.pathToSourceField
            ?: return NadelBatchHydrationArgumentMissingSourceFieldError(
                parentType = parent,
                virtualField = virtualField,
                hydration = hydration,
            ).asInterimError()

        val matchObjectIdentifiers = inputIdentifiedBy
            .map { identifier ->
                val sourceIdPath = identifier.sourceId.split(".")// e.g. context.jiraComment.id

                if (!sourceIdPath.startsWith(pathToSourceInputField)) {
                    return NadelBatchHydrationMatchingStrategyInvalidSourceIdError(
                        parentType = parent,
                        virtualField = virtualField,
                        hydration = hydration,
                        offendingObjectIdentifier = identifier,
                    ).asInterimError()
                }

                (parent.underlying as GraphQLFieldsContainer).getFieldAt(sourceIdPath)
                    ?: return NadelBatchHydrationMatchingStrategyReferencesNonExistentSourceFieldError(
                        parentType = parent,
                        virtualField = virtualField,
                        hydration = hydration,
                        pathToNonExistentSourceField = sourceIdPath,
                    ).asInterimError()

                // todo: enable validation one day
                // val backingFieldContainerType = engineSchema.queryType.getFieldContainerFor(hydration.backingField)!!
                // val backingField = engineSchema.queryType.getFieldAt(hydration.backingField)!!
                // val backingService = fieldContributor[
                //     makeFieldCoordinates(backingFieldContainerType.name, backingField.name)
                // ]!!
                // val underlyingBackingFieldPath = engineSchema.queryType.getFieldsAlong(hydration.backingField)
                //     .map {
                //         it.getRenamedOrNull()?.from?.single() ?: it.name
                //     }
                // val underlyingBackingField =
                //     backingService.underlyingSchema.queryType.getFieldAt(underlyingBackingFieldPath)!!
                // val backingFieldOutputType = (underlyingBackingField.type.unwrapAll() as GraphQLFieldsContainer)
                //
                // if (backingFieldOutputType?.getField(identifier.resultId) == null) {
                //     error("No result field found")
                // }

                NadelBatchHydrationMatchStrategy.MatchObjectIdentifier(
                    sourceId = NadelQueryPath(sourceIdPath),
                    resultId = identifier.resultId,
                )
            }

        return NadelBatchHydrationMatchStrategy.MatchObjectIdentifiers(matchObjectIdentifiers).asInterimSuccess()
    }

    context(NadelValidationContext)
    private fun limitBatchHydrationMismatch(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        hydrations: List<NadelHydrationDefinition>,
    ): NadelSchemaValidationResult {
        val (batched, notBatched) = hydrations.partitionCount {
            isBatchHydration(it)
        }

        return if (batched > 0 && notBatched > 0) {
            NadelHydrationTypeMismatchError(parent, overallField)
        } else {
            ok()
        }
    }

    context(NadelValidationContext)
    private fun limitSourceField(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        hydrations: List<NadelHydrationDefinition>,
    ): NadelSchemaValidationResult {
        if (hydrations.size > 1) {
            val pathsToSourceFields = hydrations
                .asSequence()
                .flatMap { hydration ->
                    hydration
                        .arguments
                        .asSequence()
                        .filterIsInstance<NadelHydrationArgumentDefinition.SourceField>()
                        .map { it.pathToSourceField }
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
                    return NadelPolymorphicHydrationIncompatibleSourceFieldsError(parent, overallField)
                }
            }
        }

        return ok()
    }

    context(NadelValidationContext)
    private fun limitUseOfIndexHydration(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        hydrations: List<NadelHydrationDefinition>,
    ): NadelSchemaValidationResult {
        // todo: or maybe just don't allow polymorphic index hydration
        val (indexCount, nonIndexCount) = hydrations.partitionCount { it.isIndexed }
        if (indexCount > 0 && nonIndexCount > 0) {
            return NadelHydrationMustUseIndexExclusivelyError(parent, overallField)
        }

        return ok()
    }

    context(NadelValidationContext)
    private fun isBatchHydration(hydrationDefinition: NadelHydrationDefinition): Boolean {
        val backingFieldDef = engineSchema.queryType.getFieldAt(hydrationDefinition.backingField)
            ?: return false // Error handled elsewhere

        val hasBatchedArgument = hydrationDefinition.arguments
            .asSequence()
            .filterIsInstance<NadelHydrationArgumentDefinition.SourceField>()
            .any {
                val argument = backingFieldDef.getArgument(it.backingFieldArgumentName)
                argument?.type?.unwrapNonNull()?.isList == true
            }

        return backingFieldDef.type.unwrapNonNull().isList && hasBatchedArgument
    }

    context(NadelValidationContext, NadelHydrationValidationContext)
    private fun validateOutputType(): NadelSchemaValidationResult {
        // Ensures that the underlying type of the backing field matches with the expected overall output type
        val overallType = virtualField.type.unwrapAll()

        if ((overallType as? GraphQLDirectiveContainer)?.isVirtualType() == true) {
            return ok() // Bypass validation for now
        }

        // Polymorphic hydration must have union output
        if (hasMoreThanOneHydration && overallType !is GraphQLUnionType) {
            return NadelPolymorphicHydrationMustOutputUnionError(parent, virtualField)
        }

        val acceptableOutputTypes: List<GraphQLNamedOutputType> = when (overallType) {
            is GraphQLUnionType -> overallType.types + overallType
            is GraphQLInterfaceType -> engineSchema.getImplementations(overallType) + overallType
            else -> listOf(overallType as GraphQLNamedOutputType)
        }

        val backingOutputTypes: List<GraphQLNamedOutputType> =
            when (val backingOutputType = backingField.type.unwrapAll()) {
                is GraphQLUnionType -> backingOutputType.types
                is GraphQLInterfaceType -> engineSchema.getImplementations(backingOutputType)
                else -> listOf(backingOutputType as GraphQLNamedOutputType)
            }

        backingOutputTypes
            // Find incompatible output types
            .filter { backingOutputType ->
                acceptableOutputTypes.none { acceptableOutputType ->
                    typeValidation.isAssignableTo(lhs = acceptableOutputType, rhs = backingOutputType)
                }
            }
            .map { backingOutputType ->
                NadelHydrationIncompatibleOutputTypeError(
                    parentType = parent,
                    virtualField = virtualField,
                    backingField = backingField,
                    hydration = hydrationDefinition,
                    incompatibleOutputType = backingOutputType,
                )
            }
            .onError { return it }

        // Hydrations can error out so they MUST always be nullable
        if (virtualField.type.isNonNull) {
            return NadelHydrationVirtualFieldMustBeNullableError(parent, virtualField)
        }

        return ok()
    }
}
