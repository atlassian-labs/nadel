package graphql.nadel.validation

import graphql.GraphQLContext
import graphql.language.Value
import graphql.nadel.definition.hydration.NadelBatchObjectIdentifiedByDefinition
import graphql.nadel.definition.hydration.NadelHydrationArgumentDefinition
import graphql.nadel.definition.hydration.NadelHydrationDefinition
import graphql.nadel.definition.hydration.getHydrationDefinitions
import graphql.nadel.definition.renamed.getRenamedOrNull
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
import graphql.nadel.engine.util.makeNormalizedInputValue
import graphql.nadel.engine.util.partitionCount
import graphql.nadel.engine.util.singleOfTypeOrNull
import graphql.nadel.engine.util.startsWith
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.engine.util.unwrapNonNull
import graphql.nadel.validation.NadelSchemaValidationError.CannotRenameHydratedField
import graphql.nadel.validation.NadelSchemaValidationError.DuplicatedHydrationArgument
import graphql.nadel.validation.NadelSchemaValidationError.FieldWithPolymorphicHydrationMustReturnAUnion
import graphql.nadel.validation.NadelSchemaValidationError.HydrationFieldMustBeNullable
import graphql.nadel.validation.NadelSchemaValidationError.HydrationMissingRequiredBackingFieldArgument
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationArgumentValueSource
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationBackingField
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationFieldValueSource
import graphql.nadel.validation.NadelSchemaValidationError.MultipleSourceArgsInBatchHydration
import graphql.nadel.validation.NadelSchemaValidationError.NoSourceArgsInBatchHydration
import graphql.nadel.validation.NadelSchemaValidationError.NonExistentHydrationBackingFieldArgument
import graphql.schema.GraphQLDirectiveContainer
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLNamedOutputType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLType
import graphql.schema.GraphQLUnionType
import graphql.validation.ValidationUtil
import java.math.BigInteger
import java.util.Locale

internal object NadelOnValidationErrorContext

internal inline fun List<NadelSchemaValidationResult>.onError(
    onError: NadelOnValidationErrorContext.(List<NadelSchemaValidationResult>) -> Unit,
): List<NadelSchemaValidationResult> {
    if (any(NadelSchemaValidationResult::isError)) {
        with(NadelOnValidationErrorContext) {
            onError(this@onError)
        }
    }

    return this
}

internal inline fun NadelSchemaValidationResult?.onError(
    onError: NadelOnValidationErrorContext.(NadelSchemaValidationResult) -> Unit,
): NadelSchemaValidationResult? {
    if (this?.isError == true) {
        with(NadelOnValidationErrorContext) {
            onError(this@onError)
        }
    }

    return this
}

private sealed class NadelHydrationValidationResult<T> {
    data class Success<T>(
        val data: T,
    ) : NadelHydrationValidationResult<T>()

    data class Error<T>(
        val errors: List<NadelSchemaValidationError>,
    ) : NadelHydrationValidationResult<T>() {
        constructor(
            error: NadelSchemaValidationError,
        ) : this(listOf(error))

        companion object {
            /**
             * Not a proper constructor because it conflicts with constructor(List)
             */
            context(NadelOnValidationErrorContext)
            operator fun <T> invoke(
                error: List<NadelSchemaValidationResult>,
            ) = Error<T>(
                errors = error.filterIsInstance<NadelSchemaValidationError>(),
            )
        }
    }

    inline fun onError(
        onError: (List<NadelSchemaValidationError>) -> Nothing,
    ): T {
        when (this) {
            is Error -> onError(errors)
            is Success -> return data
        }
    }
}

internal class NadelHydrationValidation(
    private val typeValidation: NadelTypeValidation,
) {
    private val validationUtil = ValidationUtil()
    private val nadelHydrationConditionValidation = NadelHydrationConditionValidation()

    context(NadelValidationContext)
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

        nadelHydrationConditionValidation
            .validateConditionsOnAllHydrations(hydrations, parent, overallField)
            .onError { return listOf(it) }

        val hasMoreThanOneHydration = hydrations.size > 1

        limitBatchHydrationMismatch(parent, overallField, hydrations)
            .onError { return it }
        limitUseOfIndexHydration(parent, overallField, hydrations)
            .onError { return it }
        limitSourceField(parent, overallField, hydrations)
            .onError { return it }

        return hydrations
            .flatMap { hydration ->
                validate(parent, overallField, hydration, hasMoreThanOneHydration)
            }
    }

    context(NadelValidationContext)
    private fun validate(
        parent: NadelServiceSchemaElement,
        virtualField: GraphQLFieldDefinition,
        hydration: NadelHydrationDefinition,
        hasMoreThanOneHydration: Boolean,
    ): List<NadelSchemaValidationResult> {
        val backingFieldContainer = engineSchema.queryType.getFieldContainerFor(hydration.backingField)
            ?: return listOf(
                MissingHydrationBackingField(parent, virtualField, hydration),
            )
        val backingFieldDef = engineSchema.queryType.getFieldAt(hydration.backingField)
            ?: return listOf(
                MissingHydrationBackingField(parent, virtualField, hydration),
            )

        validateOutputType(parent, virtualField, backingFieldDef, hasMoreThanOneHydration)
            .onError { return it }

        val arguments = validateArguments(parent, virtualField, hydration, backingFieldDef)
            .onError { return it }

        val isBatched = backingFieldDef.type.unwrapNonNull().isList

        val backingService =
            fieldContributor[makeFieldCoordinates(backingFieldContainer.name, backingFieldDef.name)]!!

        val hydrationCondition = getHydrationCondition(hydration)

        // We only look at concrete instructions, tbh this should probably error out
        if (parent.overall !is GraphQLObjectType) {
            return emptyList()
        }

        if (isBatched) {
            val matchStrategy = validateBatchMatchingStrategy(parent, virtualField, hydration)
                .onError { return it }

            return listOf(
                NadelValidatedFieldResult(
                    parent.service,
                    NadelBatchHydrationFieldInstruction(
                        location = makeFieldCoordinates(parent.overall.name, virtualField.name),
                        virtualFieldDef = virtualField,
                        backingService = backingService,
                        queryPathToBackingField = NadelQueryPath(hydration.backingField),
                        backingFieldArguments = arguments,
                        timeout = hydration.timeout,
                        sourceFields = getBatchHydrationSourceFields(arguments, matchStrategy, hydrationCondition),
                        backingFieldDef = backingFieldDef,
                        backingFieldContainer = backingFieldContainer,
                        condition = hydrationCondition,
                        virtualTypeContext = null,
                        batchSize = hydration.batchSize,
                        batchHydrationMatchStrategy = matchStrategy,
                    )
                ),
            )
        } else {
            return listOf(
                NadelValidatedFieldResult(
                    parent.service,
                    NadelHydrationFieldInstruction(
                        location = makeFieldCoordinates(parent.overall.name, virtualField.name),
                        virtualFieldDef = virtualField,
                        backingService = backingService,
                        queryPathToBackingField = NadelQueryPath(hydration.backingField),
                        backingFieldArguments = arguments,
                        timeout = hydration.timeout,
                        sourceFields = arguments.getSourceFields() + listOfNotNull(hydrationCondition?.fieldPath),
                        backingFieldDef = backingFieldDef,
                        backingFieldContainer = backingFieldContainer,
                        condition = hydrationCondition,
                        virtualTypeContext = null,
                        // todo: code properly
                        hydrationStrategy = run {
                            val manyToOneInputDef = hydration.arguments
                                .asSequence()
                                .mapNotNull { inputValueDef ->
                                    if (inputValueDef !is NadelHydrationArgumentDefinition.ObjectField) {
                                        return@mapNotNull null
                                    }

                                    val underlyingParentType =
                                        if ((parent.overall as GraphQLDirectiveContainer).isVirtualType()) {
                                            parent.overall
                                        } else {
                                            parent.underlying
                                        } as GraphQLObjectType

                                    val fieldDefs = underlyingParentType.getFieldsAlong(inputValueDef.pathToField)
                                    inputValueDef.takeIf {
                                        fieldDefs.any { fieldDef ->
                                            fieldDef.type.unwrapNonNull().isList
                                                && !backingFieldDef.getArgument(inputValueDef.name).type.unwrapNonNull().isList
                                        }
                                    }
                                }
                                .emptyOrSingle()

                            if (manyToOneInputDef != null) {
                                if (!virtualField.type.unwrapNonNull().isList) {
                                    error("Illegal hydration declaration")
                                }
                                NadelHydrationStrategy.ManyToOne(arguments.first { it.name == manyToOneInputDef.name })
                            } else {
                                NadelHydrationStrategy.OneToOne
                            }
                        },
                    )
                ),
            )
        }
    }

    context(NadelValidationContext)
    private fun getBatchHydrationSourceFields(
        arguments: List<NadelHydrationArgument>,
        matchStrategy: NadelBatchHydrationMatchStrategy,
        hydrationCondition: NadelHydrationCondition?,
    ): List<NadelQueryPath> {
        return (
            arguments.getSourceFields() +
                matchStrategy.getSourceFields() +
                listOfNotNull(hydrationCondition?.fieldPath)
            ).toSet().let { paths ->
                val prefixes = paths
                    .asSequence()
                    .map {
                        it.segments.dropLast(1) + "*"
                    }
                    .toSet()

                // Say we have paths = [
                //     [page]
                //     [page.id]
                //     [page.status]
                // ]
                // (e.g. page was the input and the page.id and page.status are used to match batch objects)
                // then this maps it to [
                //     [page.id]
                //     [page.status]
                // ]
                val sourceFieldsFromArgs = paths
                    .filter {
                        !prefixes.contains(it.segments + "*")
                    }

                sourceFieldsFromArgs
            }
    }

    /**
     * todo: merge this with the validation code
     */
    private fun getHydrationCondition(hydration: NadelHydrationDefinition): NadelHydrationCondition? {
        val resultCondition = hydration.condition?.result
            ?: return null

        if (resultCondition.predicate.equals != null) {
            return when (val expectedValue = resultCondition.predicate.equals) {
                is BigInteger -> NadelHydrationCondition.LongResultEquals(
                    fieldPath = NadelQueryPath(resultCondition.pathToSourceField),
                    value = expectedValue.longValueExact(),
                )
                is String -> NadelHydrationCondition.StringResultEquals(
                    fieldPath = NadelQueryPath(resultCondition.pathToSourceField),
                    value = expectedValue
                )
                else -> error("Unexpected type for equals predicate in conditional hydration")
            }
        }
        if (resultCondition.predicate.startsWith != null) {
            return NadelHydrationCondition.StringResultStartsWith(
                fieldPath = NadelQueryPath(resultCondition.pathToSourceField),
                prefix = resultCondition.predicate.startsWith
            )
        }
        if (resultCondition.predicate.matches != null) {
            return NadelHydrationCondition.StringResultMatches(
                fieldPath = NadelQueryPath(resultCondition.pathToSourceField),
                regex = resultCondition.predicate.matches.toRegex()
            )
        }

        error("A conditional hydration is defined but doesnt have any predicate")
    }

    context(NadelValidationContext)
    private fun validateBatchMatchingStrategy(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        hydration: NadelHydrationDefinition,
    ): NadelHydrationValidationResult<NadelBatchHydrationMatchStrategy> {
        if (hydration.isIndexed) {
            return NadelHydrationValidationResult.Success(NadelBatchHydrationMatchStrategy.MatchIndex)
        }

        val inputIdentifiedBy = hydration.inputIdentifiedBy
        return if (inputIdentifiedBy?.isNotEmpty() == true) {
            validateInputIdentifiedByMatchingStrategy(
                parent,
                overallField,
                hydration,
                inputIdentifiedBy,
            )
        } else {
            val identifiedBy = hydration.identifiedBy
                ?: error("No identified by strategy") // todo: add proper error here

            val backingFieldContainerType = engineSchema.queryType.getFieldContainerFor(hydration.backingField)!!
            val backingField = engineSchema.queryType.getFieldAt(hydration.backingField)!!
            val backingService = fieldContributor[
                makeFieldCoordinates(backingFieldContainerType.name, backingField.name)
            ]!!
            val underlyingBackingFieldPath = engineSchema.queryType.getFieldsAlong(hydration.backingField)
                .map {
                    it.getRenamedOrNull()?.from?.single() ?: it.name
                }
            val underlyingBackingField =
                backingService.underlyingSchema.queryType.getFieldAt(underlyingBackingFieldPath)!!
            val backingFieldOutputType = underlyingBackingField.type.unwrapAll()

            // todo: enable validation one day
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

            NadelHydrationValidationResult.Success(
                NadelBatchHydrationMatchStrategy.MatchObjectIdentifier(
                    sourceId = NadelQueryPath(
                        hydration
                            .arguments
                            .asSequence()
                            .filterIsInstance<NadelHydrationArgumentDefinition.ObjectField>()
                            .single()
                            .pathToField,
                    ),
                    resultId = hydration.identifiedBy!!
                )
            )
        }
    }

    context(NadelValidationContext)
    private fun validateInputIdentifiedByMatchingStrategy(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        hydration: NadelHydrationDefinition,
        inputIdentifiedBy: List<NadelBatchObjectIdentifiedByDefinition>,
    ): NadelHydrationValidationResult<NadelBatchHydrationMatchStrategy> {
        val pathToSourceInputField = hydration.arguments
            .singleOfTypeOrNull<NadelHydrationArgumentDefinition.ObjectField>()
            ?.pathToField
            ?: return NadelHydrationValidationResult.Error(
                NoSourceArgsInBatchHydration(
                    parentType = parent,
                    overallField = overallField,
                ),
            )

        val matchObjectIdentifiers = inputIdentifiedBy
            .map { identifier ->
                val sourceIdPath = identifier.sourceId.split(".")// e.g. context.jiraComment.id

                if (!sourceIdPath.startsWith(pathToSourceInputField)) {
                    return NadelHydrationValidationResult.Error(
                        NadelSchemaValidationError.ObjectIdentifierMustFollowSourceInputField(
                            type = parent,
                            field = overallField,
                            pathToSourceInputField = pathToSourceInputField,
                            offendingObjectIdentifier = identifier,
                        ),
                    )
                }

                (parent.underlying as GraphQLFieldsContainer)
                    .getFieldAt(sourceIdPath)
                    ?: error("Source ID does not exist") // todo: make up error here

                val backingFieldContainerType = engineSchema.queryType.getFieldContainerFor(hydration.backingField)!!
                val backingField = engineSchema.queryType.getFieldAt(hydration.backingField)!!
                val backingService = fieldContributor[
                    makeFieldCoordinates(backingFieldContainerType.name, backingField.name)
                ]!!
                val underlyingBackingFieldPath = engineSchema.queryType.getFieldsAlong(hydration.backingField)
                    .map {
                        it.getRenamedOrNull()?.from?.single() ?: it.name
                    }
                val underlyingBackingField =
                    backingService.underlyingSchema.queryType.getFieldAt(underlyingBackingFieldPath)!!
                val backingFieldOutputType = (underlyingBackingField.type.unwrapAll() as GraphQLFieldsContainer)

                // todo: enable validation one day
                // if (backingFieldOutputType?.getField(identifier.resultId) == null) {
                //     error("No result field found")
                // }

                NadelBatchHydrationMatchStrategy.MatchObjectIdentifier(
                    sourceId = NadelQueryPath(sourceIdPath),
                    resultId = identifier.resultId,
                )
            }

        return NadelHydrationValidationResult.Success(
            NadelBatchHydrationMatchStrategy.MatchObjectIdentifiers(matchObjectIdentifiers),
        )
    }

    context(NadelValidationContext)
    private fun limitBatchHydrationMismatch(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        hydrations: List<NadelHydrationDefinition>,
    ): List<NadelSchemaValidationResult> {
        val (batched, notBatched) = hydrations.partitionCount {
            isBatched(it)
        }

        return if (batched > 0 && notBatched > 0) {
            listOf(
                NadelSchemaValidationError.HydrationsMismatch(parent, overallField),
            )
        } else {
            emptyList()
        }
    }

    context(NadelValidationContext)
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

    context(NadelValidationContext)
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

    context(NadelValidationContext)
    private fun isBatched(hydration: NadelHydrationDefinition): Boolean {
        val backingFieldDef = engineSchema.queryType.getFieldAt(hydration.backingField)
        return hydration.isBatched || /*deprecated*/ backingFieldDef?.type?.unwrapNonNull()?.isList == true
    }

    context(NadelValidationContext)
    private fun validateOutputType(
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
            is GraphQLInterfaceType -> engineSchema.getImplementations(overallType) + overallType
            else -> listOf(overallType as GraphQLNamedOutputType)
        }

        val backingOutputTypes: List<GraphQLNamedOutputType> =
            when (val backingOutputType = backingField.type.unwrapAll()) {
                is GraphQLUnionType -> backingOutputType.types
                is GraphQLInterfaceType -> engineSchema.getImplementations(backingOutputType)
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

    context(NadelValidationContext)
    private fun validateArguments(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        hydration: NadelHydrationDefinition,
        backingField: GraphQLFieldDefinition,
    ): NadelHydrationValidationResult<List<NadelHydrationArgument>> {
        // Can only provide one value for an argument
        validateDuplicateArguments(parent, overallField, hydration)
            .onError {
                return NadelHydrationValidationResult.Error(it)
            }
        validateRequiredBackingArguments(backingField, hydration, parent, overallField)
            .onError {
                return NadelHydrationValidationResult.Error(it)
            }
        validateBatchHydrationArguments(parent, overallField, hydration, backingField)
            .onError {
                return NadelHydrationValidationResult.Error(it)
            }

        return NadelHydrationValidationResult.Success(
            data = hydration.arguments
                .map { hydrationArgument ->
                    validateArgument(
                        parent = parent,
                        overallField = overallField,
                        hydration = hydration,
                        backingField = backingField,
                        hydrationArgument = hydrationArgument,
                    ).onError {
                        return NadelHydrationValidationResult.Error(it)
                    }
                },
        )
    }

    context(NadelValidationContext)
    private fun validateArgument(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        hydration: NadelHydrationDefinition,
        backingField: GraphQLFieldDefinition,
        hydrationArgument: NadelHydrationArgumentDefinition,
    ): NadelHydrationValidationResult<NadelHydrationArgument> {
        val backingArgumentDefinition = backingField.getArgument(hydrationArgument.name)
            ?: return NadelHydrationValidationResult.Error(
                NonExistentHydrationBackingFieldArgument(
                    parent,
                    overallField,
                    hydration,
                    argument = hydrationArgument.name,
                ),
            )

        val isBatchHydration = backingField.type.unwrapNonNull().isList

        return when (hydrationArgument) {
            is NadelHydrationArgumentDefinition.ObjectField -> {
                getObjectFieldArgumentErrors(
                    parent,
                    overallField,
                    backingField,
                    hydration,
                    hydrationArgument,
                    isBatchHydration
                ).onError {
                    return NadelHydrationValidationResult.Error(it.filterIsInstance<NadelSchemaValidationError>())
                }

                NadelHydrationValidationResult.Success(
                    NadelHydrationArgument(
                        name = hydrationArgument.name,
                        backingArgumentDef = backingArgumentDefinition,
                        valueSource = NadelHydrationArgument.ValueSource.FieldResultValue(
                            // todo: return error if not found
                            fieldDefinition = (parent.underlying as GraphQLFieldsContainer).getFieldAt(hydrationArgument.pathToField)!!,
                            queryPathToField = NadelQueryPath(hydrationArgument.pathToField),
                        )
                    ),
                )
            }
            is NadelHydrationArgumentDefinition.FieldArgument -> {
                getFieldArgumentErrors(
                    parent,
                    overallField,
                    backingField,
                    hydration,
                    hydrationArgument,
                    isBatchHydration,
                ).onError {
                    return NadelHydrationValidationResult.Error(it.filterIsInstance<NadelSchemaValidationError>())
                }

                val argumentDefinition = overallField.getArgument(hydrationArgument.argumentName)!!
                val defaultValue = if (argumentDefinition.argumentDefaultValue.isLiteral) {
                    makeNormalizedInputValue(
                        argumentDefinition.type,
                        argumentDefinition.argumentDefaultValue.value as Value<*>,
                    )
                } else {
                    null
                }

                NadelHydrationValidationResult.Success(
                    NadelHydrationArgument(
                        name = hydrationArgument.name,
                        backingArgumentDef = backingArgumentDefinition,
                        valueSource = NadelHydrationArgument.ValueSource.ArgumentValue(
                            argumentName = hydrationArgument.argumentName,
                            argumentDefinition = argumentDefinition,
                            defaultValue = defaultValue,
                        )
                    ),
                )
            }
            is NadelHydrationArgumentDefinition.StaticArgument -> {
                getStaticArgumentErrors(
                    parent,
                    overallField,
                    backingField,
                    hydration,
                    hydrationArgument,
                ).onError {
                    return NadelHydrationValidationResult.Error(it.filterIsInstance<NadelSchemaValidationError>())
                }

                NadelHydrationValidationResult.Success(
                    NadelHydrationArgument(
                        name = hydrationArgument.name,
                        backingArgumentDef = backingArgumentDefinition,
                        valueSource = NadelHydrationArgument.ValueSource.StaticValue(
                            value = hydrationArgument.staticValue,
                        )
                    ),
                )
            }
        }
    }

    context(NadelValidationContext)
    private fun validateRequiredBackingArguments(
        backingField: GraphQLFieldDefinition,
        hydration: NadelHydrationDefinition,
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
    ): List<HydrationMissingRequiredBackingFieldArgument> {
        return backingField.arguments
            .filter { it.type.isNonNull }
            .mapNotNull { backingArg ->
                val hydrationArg = hydration.arguments.find { it.name == backingArg.name }
                if (hydrationArg == null) {
                    HydrationMissingRequiredBackingFieldArgument(
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

    context(NadelValidationContext)
    private fun validateBatchHydrationArguments(
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

    context(NadelValidationContext)
    private fun validateDuplicateArguments(
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

    context(NadelValidationContext)
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

    context(NadelValidationContext)
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

    context(NadelValidationContext)
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
                isBatchHydration = isBatchHydration,
                hydrationArgumentDefinition = hydrationArgumentDefinition,
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
                        NadelSchemaValidationError.HydrationArgumentMissingRequiredInputObjectField(
                            parentType = parent,
                            overallField = overallField,
                            hydration = hydrationDefinition,
                            hydrationArgument = hydrationArgumentDefinition,
                            suppliedFieldContainer = it.suppliedFieldContainer,
                            requiredFieldContainer = it.requiredFieldContainer,
                            requiredField = it.requiredField,
                        )
                    }
                    NadelHydrationArgumentTypeValidationResult.InvalidBatchIdArgument -> {
                        NadelSchemaValidationError.BatchHydrationInvalidBatchingArgument(
                            parentType = parent,
                            overallField = overallField,
                            hydration = hydrationDefinition,
                            hydrationArgument = hydrationArgumentDefinition,
                        )
                    }
                }
            }

        return null
    }

    context(NadelValidationContext)
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
                engineSchema,
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

context(NadelValidationContext)
private fun List<NadelHydrationArgument>.getSourceFields(): List<NadelQueryPath> {
    return flatMap {
        when (it.valueSource) {
            is NadelHydrationArgument.ValueSource.ArgumentValue -> emptyList()
            is NadelHydrationArgument.ValueSource.FieldResultValue -> selectSourceFieldQueryPaths(it.valueSource)
            is NadelHydrationArgument.ValueSource.StaticValue -> emptyList()
            is NadelHydrationArgument.ValueSource.RemainingArguments -> emptyList()
        }
    }
}

private fun selectSourceFieldQueryPaths(
    hydrationValueSource: NadelHydrationArgument.ValueSource.FieldResultValue,
): List<NadelQueryPath> {
    val hydrationSourceType = hydrationValueSource.fieldDefinition.type.unwrapAll()
    if (hydrationSourceType is GraphQLObjectType) {
        // When the argument of the hydration backing field is an input type and not a primitive
        // we need to add all the input fields to the source fields
        return hydrationSourceType.fields.map { field ->
            hydrationValueSource.queryPathToField.plus(field.name)
        }
    }
    return listOf(hydrationValueSource.queryPathToField)
}

private fun NadelBatchHydrationMatchStrategy.getSourceFields(): List<NadelQueryPath> {
    return when (this) {
        is NadelBatchHydrationMatchStrategy.MatchIndex -> emptyList()
        is NadelBatchHydrationMatchStrategy.MatchObjectIdentifier -> listOf(sourceId)
        is NadelBatchHydrationMatchStrategy.MatchObjectIdentifiers -> objectIds.map { it.sourceId }
    }
}
