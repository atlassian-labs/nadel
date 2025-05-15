package graphql.nadel.validation.hydration

import graphql.Scalars.GraphQLID
import graphql.Scalars.GraphQLString
import graphql.nadel.Service
import graphql.nadel.definition.hydration.NadelBatchObjectIdentifiedByDefinition
import graphql.nadel.definition.hydration.NadelHydrationArgumentDefinition
import graphql.nadel.definition.hydration.NadelHydrationDefinition
import graphql.nadel.definition.virtualType.hasVirtualTypeDefinition
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.blueprint.hydration.NadelHydrationArgument
import graphql.nadel.engine.blueprint.hydration.NadelHydrationCondition
import graphql.nadel.engine.blueprint.hydration.NadelHydrationStrategy
import graphql.nadel.engine.blueprint.hydration.NadelObjectIdentifierCastingStrategy
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
import graphql.nadel.engine.util.whenType
import graphql.nadel.validation.NadelBatchHydrationArgumentMissingSourceFieldError
import graphql.nadel.validation.NadelBatchHydrationMatchingStrategyInvalidSourceIdError
import graphql.nadel.validation.NadelBatchHydrationMatchingStrategyReferencesNonExistentSourceFieldError
import graphql.nadel.validation.NadelBatchHydrationMissingIdentifiedByError
import graphql.nadel.validation.NadelHydrationCannotSqueezeSourceListError
import graphql.nadel.validation.NadelHydrationIncompatibleOutputTypeError
import graphql.nadel.validation.NadelHydrationMustUseIndexExclusivelyError
import graphql.nadel.validation.NadelHydrationReferencesNonExistentBackingFieldError
import graphql.nadel.validation.NadelHydrationTypeMismatchError
import graphql.nadel.validation.NadelHydrationUnionMemberNoBackingError
import graphql.nadel.validation.NadelHydrationVirtualFieldMustBeNullableError
import graphql.nadel.validation.NadelPolymorphicHydrationIncompatibleSourceFieldsError
import graphql.nadel.validation.NadelPolymorphicHydrationMustOutputUnionError
import graphql.nadel.validation.NadelSchemaValidationError.CannotRenameHydratedField
import graphql.nadel.validation.NadelSchemaValidationResult
import graphql.nadel.validation.NadelServiceSchemaElement
import graphql.nadel.validation.NadelValidatedFieldResult
import graphql.nadel.validation.NadelValidationContext
import graphql.nadel.validation.NadelValidationInterimResult
import graphql.nadel.validation.NadelValidationInterimResult.Error.Companion.asInterimError
import graphql.nadel.validation.NadelValidationInterimResult.Success.Companion.asInterimSuccess
import graphql.nadel.validation.ok
import graphql.nadel.validation.onError
import graphql.nadel.validation.onErrorCast
import graphql.nadel.validation.results
import graphql.nadel.validation.toResult
import graphql.schema.GraphQLDirectiveContainer
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLImplementingType
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLNamedOutputType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLUnionType

internal data class NadelHydrationValidationContext(
    val hasMoreThanOneHydration: Boolean,
    val parent: NadelServiceSchemaElement.FieldsContainer,
    val virtualField: GraphQLFieldDefinition,
    val hydrationDefinition: NadelHydrationDefinition,
    val isBatchHydration: Boolean,
    val backingFieldContainer: GraphQLFieldsContainer,
    val backingField: GraphQLFieldDefinition,
)

class NadelHydrationValidation internal constructor(
    private val argumentValidation: NadelHydrationArgumentValidation,
    private val conditionValidation: NadelHydrationConditionValidation,
    private val sourceFieldValidation: NadelHydrationSourceFieldValidation,
    private val virtualTypeValidation: NadelHydrationVirtualTypeValidation,
) {

    context(NadelValidationContext)
    fun validate(
        parent: NadelServiceSchemaElement.FieldsContainer,
        virtualField: GraphQLFieldDefinition,
    ): NadelSchemaValidationResult {
        if (instructionDefinitions.isRenamed(parent, virtualField)) {
            return CannotRenameHydratedField(parent, virtualField)
        }

        val hydrations = instructionDefinitions.getHydrationDefinitions(parent, virtualField).toList()
        if (hydrations.isEmpty()) {
            error("Don't invoke hydration validation if there is no hydration silly")
        }

        return validate(
            parent = parent,
            virtualField = virtualField,
            hydrations = hydrations,
        )
    }

    context(NadelValidationContext)
    private fun validate(
        parent: NadelServiceSchemaElement.FieldsContainer,
        virtualField: GraphQLFieldDefinition,
        hydrations: List<NadelHydrationDefinition>,
    ): NadelSchemaValidationResult {
        conditionValidation
            .validateHydrations(hydrations, parent, virtualField)
            .onError { return it }

        val hasMoreThanOneHydration = hydrations.size > 1

        limitBatchHydrationMismatch(parent, virtualField, hydrations)
            .onError { return it }
        limitUseOfIndexHydration(parent, virtualField, hydrations)
            .onError { return it }
        limitSourceField(parent, virtualField, hydrations)
            .onError { return it }

        return results(
            validateUnionMembersBacked(parent, virtualField, hydrations), // Do this instead of .onError for a soft error
            hydrations
                .map { hydration ->
                    validate(parent, virtualField, hydration, hasMoreThanOneHydration)
                }
                .toResult(),
        )
    }

    /**
     * Checks that if the virtual field outputs a union, that all of its union members have
     * a corresponding backing field that can resolve it.
     */
    context(NadelValidationContext)
    private fun validateUnionMembersBacked(
        parent: NadelServiceSchemaElement.FieldsContainer,
        virtualField: GraphQLFieldDefinition,
        hydrations: List<NadelHydrationDefinition>,
    ): NadelSchemaValidationResult {
        val unionType = virtualField.type.unwrapAll() as? GraphQLUnionType
            ?: return ok()

        val suppliedTypes = hydrations
            .flatMap { hydration ->
                val backingField = engineSchema.queryType.getFieldAt(hydration.backingField)
                    ?: return NadelHydrationReferencesNonExistentBackingFieldError(
                        parent,
                        virtualField,
                        hydration,
                    )

                backingField.type.unwrapAll()
                    .whenType(
                        enumType = ::listOf,
                        inputObjectType = ::listOf,
                        interfaceType = engineSchema::getImplementations,
                        objectType = ::listOf,
                        scalarType = ::listOf,
                        unionType = GraphQLUnionType::getTypes,
                    )
            }

        return if (suppliedTypes.containsAll(unionType.types)) {
            ok()
        } else {
            NadelHydrationUnionMemberNoBackingError(parent, virtualField, unionType.types - suppliedTypes.toSet())
        }
    }

    context(NadelValidationContext)
    private fun validate(
        parent: NadelServiceSchemaElement.FieldsContainer,
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

        val arguments = argumentValidation.validateArguments(isBatchHydration)
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
        val virtualTypeContext = virtualTypeValidation.getVirtualTypeContext()
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
                virtualTypeContext = virtualTypeContext,
                hydrationStrategy = hydrationStrategy,
            ),
        )
    }

    context(NadelValidationContext, NadelHydrationValidationContext)
    private fun validateNonBatchHydrationStrategy(
        arguments: List<NadelHydrationArgument>,
    ): NadelValidationInterimResult<NadelHydrationStrategy> {
        val manyToOneInputDef = hydrationDefinition.arguments
            .asSequence()
            .mapNotNull { inputValueDef ->
                if (inputValueDef !is NadelHydrationArgumentDefinition.ObjectField) {
                    return@mapNotNull null
                }

                val underlyingParentType =
                    if ((parent.overall as GraphQLDirectiveContainer).hasVirtualTypeDefinition()) {
                        parent.overall
                    } else {
                        parent.underlying
                    } as GraphQLObjectType

                val fieldDefs = underlyingParentType.getFieldsAlong(inputValueDef.pathToField)
                inputValueDef.takeIf {
                    fieldDefs.any { fieldDef ->
                        fieldDef.type.unwrapNonNull().isList
                            && !backingField.getArgument(inputValueDef.name).type.unwrapNonNull().isList
                    }
                }
            }
            .emptyOrSingle()

        return if (manyToOneInputDef == null) {
            NadelHydrationStrategy.OneToOne
        } else {
            if (!virtualField.type.unwrapNonNull().isList) {
                NadelHydrationCannotSqueezeSourceListError(
                    parentType = parent,
                    virtualField = virtualField,
                    hydration = hydrationDefinition,
                    sourceField = manyToOneInputDef,
                )
            }

            NadelHydrationStrategy.ManyToOne(arguments.first { it.name == manyToOneInputDef.name })
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
                sourceIdCast = NadelObjectIdentifierCastingStrategy.NO_CAST,
                sourceId = NadelQueryPath(
                    hydrationDefinition
                        .arguments
                        .asSequence()
                        .filterIsInstance<NadelHydrationArgumentDefinition.ObjectField>()
                        .single()
                        .pathToField,
                ),
                resultId = identifiedBy,
            ).asInterimSuccess()
        }
    }

    context(NadelValidationContext, NadelHydrationValidationContext)
    private fun getBatchIdentifiedBy(): NadelValidationInterimResult<String> {
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
            .singleOfTypeOrNull<NadelHydrationArgumentDefinition.ObjectField>()
            ?.pathToField
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
                    sourceIdCast = NadelObjectIdentifierCastingStrategy.NO_CAST,
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
            .filterIsInstance<NadelHydrationArgumentDefinition.ObjectField>()
            .any {
                val argument = backingFieldDef.getArgument(it.name)
                argument?.type?.unwrapNonNull()?.isList == true
            }

        return backingFieldDef.type.unwrapNonNull().isList && hasBatchedArgument
    }

    context(NadelValidationContext, NadelHydrationValidationContext)
    private fun validateOutputType(): NadelSchemaValidationResult {
        // Ensures that the underlying type of the backing field matches with the expected overall output type
        val overallType = virtualField.type.unwrapAll()

        if ((overallType as? GraphQLDirectiveContainer)?.hasVirtualTypeDefinition() == true) {
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
                    isAssignableTo(lhs = acceptableOutputType, rhs = backingOutputType)
                }
            }
            .map { backingOutputType ->
                NadelHydrationIncompatibleOutputTypeError(
                    parentType = parent,
                    virtualField = virtualField,
                    backingField = backingField,
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

    /**
     * Answers whether `rhs` assignable to `lhs`?
     *
     * i.e. does the following compile
     *
     * ```
     * vol output: lhs = rhs
     * ```
     *
     * Note: this assumes both types are from the same schema. This does NOT
     * deal with differences between overall and underlying schema.
     */
    context(NadelValidationContext)
    private fun isAssignableTo(lhs: GraphQLNamedOutputType, rhs: GraphQLNamedOutputType): Boolean {
        if (lhs.name == rhs.name) {
            return true
        }
        if (lhs.name == GraphQLID.name && rhs.name == GraphQLString.name) {
            return true
        }
        if (lhs is GraphQLInterfaceType && rhs is GraphQLImplementingType) {
            return rhs.interfaces.contains(lhs)
        }
        return false
    }
}
