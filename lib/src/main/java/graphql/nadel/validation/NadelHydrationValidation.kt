package graphql.nadel.validation

import graphql.GraphQLContext
import graphql.nadel.Service
import graphql.nadel.dsl.RemoteArgumentDefinition
import graphql.nadel.dsl.RemoteArgumentSource
import graphql.nadel.engine.blueprint.directives.isVirtualType
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
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationActorField
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationActorService
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationArgumentValueSource
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationFieldValueSource
import graphql.nadel.validation.NadelSchemaValidationError.MissingRequiredHydrationActorFieldArgument
import graphql.nadel.validation.NadelSchemaValidationError.MultipleSourceArgsInBatchHydration
import graphql.nadel.validation.NadelSchemaValidationError.NoSourceArgsInBatchHydration
import graphql.nadel.validation.NadelSchemaValidationError.NonExistentHydrationActorFieldArgument
import graphql.nadel.validation.util.NadelSchemaUtil.getHydrations
import graphql.nadel.validation.util.NadelSchemaUtil.hasRename
import graphql.schema.GraphQLDirectiveContainer
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLNamedOutputType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLUnionType
import graphql.validation.ValidationUtil
import java.util.Locale

internal class NadelHydrationValidation(
    private val services: Map<String, Service>,
    private val typeValidation: NadelTypeValidation,
    private val overallSchema: GraphQLSchema,
) {
    private val validationUtil = ValidationUtil()
    private val nadelHydrationArgumentValidation = NadelHydrationArgumentValidation()
    private val nadelHydrationConditionValidation = NadelHydrationConditionValidation()

    fun validate(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
    ): List<NadelSchemaValidationError> {
        if (hasRename(overallField)) {
            return listOf(
                CannotRenameHydratedField(parent, overallField),
            )
        }

        val hydrations = getHydrations(overallField, overallSchema)
        if (hydrations.isEmpty()) {
            error("Don't invoke hydration validation if there is no hydration silly")
        }
        val whenConditionValidationError =
            nadelHydrationConditionValidation.validateConditionsOnAllHydrations(hydrations, parent, overallField)
        if (whenConditionValidationError != null) {
            return listOf(whenConditionValidationError)
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
    ): List<NadelSchemaValidationError> {
        if (hydration.serviceName !in services) {
            return listOf(
                MissingHydrationActorService(parent, overallField, hydration),
            )
        }

        val actorField = overallSchema.queryType.getFieldAt(hydration.pathToActorField)
            ?: return listOf(
                MissingHydrationActorField(parent, overallField, hydration),
            )

        return getArgumentErrors(parent, overallField, hydration, actorField) +
            getOutputTypeErrors(parent, overallField, actorField, hasMoreThanOneHydration) +
            getObjectIdentifierErrors(parent, overallField, hydration)
    }

    private fun getObjectIdentifierErrors(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        hydration: NadelHydrationDefinition,
    ): List<NadelSchemaValidationError> {
        // e.g. context.jiraComment
        val pathToSourceInputField = hydration.arguments
            .map { arg -> arg.remoteArgumentSource }
            .singleOfTypeOrNull<RemoteArgumentSource.ObjectField>()
            ?.pathToField
            ?: return emptyList() // Ignore this, checked elsewhere

        // Nothing to check
        if (hydration.objectIdentifiers == null) {
            return emptyList()
        }

        // Find offending object identifiers and generate errors
        return hydration.objectIdentifiers
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
    ): List<NadelSchemaValidationError> {
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
    ): List<NadelSchemaValidationError> {
        if (hydrations.size > 1) {
            val pathsToSourceFields = hydrations
                .asSequence()
                .flatMap { hydration ->
                    hydration
                        .arguments
                        .asSequence()
                        .map { it.remoteArgumentSource }
                        .filterIsInstance<RemoteArgumentSource.ObjectField>()
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
    ): List<NadelSchemaValidationError> {
        // todo: or maybe just don't allow polymorphic index hydration
        val (indexCount, nonIndexCount) = hydrations.partitionCount { it.isObjectMatchByIndex }
        if (indexCount > 0 && nonIndexCount > 0) {
            return listOf(
                NadelSchemaValidationError.MixedIndexHydration(parent, overallField),
            )
        }

        return emptyList()
    }

    private fun isBatched(hydration: NadelHydrationDefinition): Boolean {
        val actorFieldDef = overallSchema.queryType.getFieldAt(hydration.pathToActorField)
        return hydration.isBatched || /*deprecated*/ actorFieldDef?.type?.unwrapNonNull()?.isList == true
    }

    private fun getOutputTypeErrors(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        actorField: GraphQLFieldDefinition,
        hasMoreThanOneHydration: Boolean,
    ): List<NadelSchemaValidationError> {
        // Ensures that the underlying type of the actor field matches with the expected overall output type
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

        val actorOutputTypes: List<GraphQLNamedOutputType> = when (val actorOutputType = actorField.type.unwrapAll()) {
            is GraphQLUnionType -> actorOutputType.types
            is GraphQLInterfaceType -> overallSchema.getImplementations(actorOutputType)
            else -> listOf(actorOutputType as GraphQLNamedOutputType)
        }

        val typeValidation = actorOutputTypes
            // Find incompatible output types
            .filter { actorOutputType ->
                acceptableOutputTypes.none { acceptableOutputType ->
                    typeValidation.isAssignableTo(lhs = acceptableOutputType, rhs = actorOutputType)
                }
            }
            .map { actorOutputType ->
                NadelSchemaValidationError.HydrationIncompatibleOutputType(
                    parentType = parent,
                    overallField = overallField,
                    actorField = actorField,
                    incompatibleOutputType = actorOutputType,
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
        actorField: GraphQLFieldDefinition,
    ): List<NadelSchemaValidationError> {
        // Can only provide one value for an argument
        val duplicatedArgumentsErrors = hydration.arguments
            .groupBy { it.name }
            .filterValues { it.size > 1 }
            .values
            .map {
                DuplicatedHydrationArgument(parent, overallField, it)
            }

        val remoteArgErrors = hydration.arguments.flatMap { remoteArg ->
            val actorFieldArgument = actorField.getArgument(remoteArg.name)
            if (actorFieldArgument == null) {
                listOf(
                    NonExistentHydrationActorFieldArgument(
                        parent,
                        overallField,
                        hydration,
                        argument = remoteArg.name,
                    )
                )
            } else {
                getRemoteArgErrors(parent, overallField, remoteArg, actorField, hydration)
            }
        }

        val missingActorArgErrors = actorField.arguments
            .filter { it.type.isNonNull }
            .mapNotNull { actorArg ->
                val hydrationArg = hydration.arguments.find { it.name == actorArg.name }
                if (hydrationArg == null) {
                    MissingRequiredHydrationActorFieldArgument(
                        parent,
                        overallField,
                        hydration,
                        argument = actorArg.name,
                    )
                } else {
                    null
                }
            }

        val isBatchHydration = actorField.type.unwrapNonNull().isList
        val batchHydrationArgumentErrors: List<NadelSchemaValidationError> = when {
            isBatchHydration -> {
                val numberOfSourceArgs =
                    hydration.arguments.count { it.remoteArgumentSource is RemoteArgumentSource.ObjectField }
                when {
                    numberOfSourceArgs > 1 ->
                        listOf(MultipleSourceArgsInBatchHydration(parent, overallField))

                    numberOfSourceArgs == 0 ->
                        listOf(NoSourceArgsInBatchHydration(parent, overallField))

                    else -> emptyList()
                }
            }

            else -> emptyList()
        }
        return duplicatedArgumentsErrors + remoteArgErrors + missingActorArgErrors + batchHydrationArgumentErrors
    }

    private fun getRemoteArgErrors(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        remoteArgDef: RemoteArgumentDefinition,
        actorField: GraphQLFieldDefinition,
        hydration: NadelHydrationDefinition,
    ): List<NadelSchemaValidationError> {
        val remoteArgSource = remoteArgDef.remoteArgumentSource
        val actorFieldArg = actorField.getArgument(remoteArgDef.name)
        val isBatchHydration = actorField.type.unwrapNonNull().isList
        return when (remoteArgSource) {
            is RemoteArgumentSource.ObjectField -> {
                val field = (parent.underlying as GraphQLFieldsContainer).getFieldAt(remoteArgSource.pathToField)
                if (field == null) {
                    listOf(
                        MissingHydrationFieldValueSource(parent, overallField, remoteArgSource)
                    )
                } else {
                    listOfNotNull(
                        nadelHydrationArgumentValidation.validateHydrationInputArg(
                            field.type,
                            actorFieldArg.type,
                            parent,
                            overallField,
                            remoteArgDef,
                            hydration,
                            isBatchHydration,
                            actorField.name
                        ),
                        nadelHydrationConditionValidation.validateHydrationCondition(
                            parent,
                            overallField,
                            hydration
                        ),
                    )
                }
            }
            is RemoteArgumentSource.FieldArgument -> {
                val argument = overallField.getArgument(remoteArgSource.argumentName)
                if (argument == null) {
                    listOf(MissingHydrationArgumentValueSource(parent, overallField, remoteArgSource))
                } else {
                    // Check the input types match with hydration and actor fields
                    val hydrationArgType = argument.type
                    listOfNotNull(
                        nadelHydrationArgumentValidation.validateHydrationInputArg(
                            hydrationArgType,
                            actorFieldArg.type,
                            parent,
                            overallField,
                            remoteArgDef,
                            hydration,
                            isBatchHydration,
                            actorField.name
                        )
                    )
                }
            }
            is RemoteArgumentSource.StaticArgument -> {
                val staticArg = remoteArgSource.staticValue
                if (
                    !validationUtil.isValidLiteralValue(
                        staticArg,
                        actorFieldArg.type,
                        overallSchema,
                        GraphQLContext.getDefault(),
                        Locale.getDefault()
                    )
                ) {
                    listOf(
                        NadelSchemaValidationError.StaticArgIsNotAssignable(
                            parent,
                            overallField,
                            remoteArgDef,
                            actorFieldArg.type,
                            actorField.name
                        )
                    )
                } else {
                    emptyList()
                }
            }
        }
    }
}
