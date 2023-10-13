package graphql.nadel.validation

import graphql.GraphQLContext
import graphql.nadel.Service
import graphql.nadel.dsl.RemoteArgumentDefinition
import graphql.nadel.dsl.RemoteArgumentSource.SourceType.FieldArgument
import graphql.nadel.dsl.RemoteArgumentSource.SourceType.ObjectField
import graphql.nadel.dsl.RemoteArgumentSource.SourceType.StaticArgument
import graphql.nadel.dsl.UnderlyingServiceHydration
import graphql.nadel.engine.util.*
import graphql.nadel.validation.NadelHydrationArgumentValidation.Companion.validateHydrationInputArg
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
import graphql.schema.*
import graphql.validation.ValidationUtil
import java.util.*

internal class NadelHydrationValidation(
        private val services: Map<String, Service>,
        private val typeValidation: NadelTypeValidation,
        private val overallSchema: GraphQLSchema,
) {
    private val validationUtil = ValidationUtil()
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

        val hasMoreThanOneHydration = hydrations.size > 1
        val errors = mutableListOf<NadelSchemaValidationError>()
        for (hydration in hydrations) {
            val actorService = services[hydration.serviceName]
            if (actorService == null) {
                errors.add(MissingHydrationActorService(parent, overallField, hydration))
                continue
            }

            val actorField = overallSchema.queryType.getFieldAt(hydration.pathToActorField)
            if (actorField == null) {
                errors.add(MissingHydrationActorField(parent, overallField, hydration))
                continue
            }

            val argumentIssues = getArgumentErrors(parent, overallField, hydration, actorField)
            val outputTypeIssues = getOutputTypeIssues(parent, overallField, actorField, hasMoreThanOneHydration)
            errors.addAll(argumentIssues)
            errors.addAll(outputTypeIssues)
        }

        if (hasMoreThanOneHydration) {
            val (batched, notBatched) = hydrations.partition(::isBatched)
            if (batched.isNotEmpty() && notBatched.isNotEmpty()) {
                errors.add(NadelSchemaValidationError.HydrationsMismatch(parent, overallField))
            }
        }

        return errors
    }

    private fun isBatched(hydration: UnderlyingServiceHydration): Boolean {
        val actorFieldDef = overallSchema.queryType.getFieldAt(hydration.pathToActorField)
        return hydration.isBatched || /*deprecated*/ actorFieldDef?.type?.unwrapNonNull()?.isList == true
    }

    private fun getOutputTypeIssues(
            parent: NadelServiceSchemaElement,
            overallField: GraphQLFieldDefinition,
            actorField: GraphQLFieldDefinition,
            hasMoreThanOneHydration: Boolean,
    ): List<NadelSchemaValidationError> {
        // Ensures that the underlying type of the actor field matches with the expected overall output type
        val overallType = overallField.type.unwrapAll()

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
            hydration: UnderlyingServiceHydration,
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

        val remoteArgErrors = hydration.arguments.mapNotNull { remoteArg ->
            val actorFieldArgument = actorField.getArgument(remoteArg.name)
            if (actorFieldArgument == null) {
                //LIKE THIS
                NonExistentHydrationActorFieldArgument(
                        parent,
                        overallField,
                        hydration,
                        argument = remoteArg.name,
                )
            } else {
                val remoteArgSource = remoteArg.remoteArgumentSource
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
                val numberOfSourceArgs = hydration.arguments.count { it.remoteArgumentSource.sourceType == ObjectField }
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
            hydration: UnderlyingServiceHydration
    ): NadelSchemaValidationError? {
        val remoteArgSource = remoteArgDef.remoteArgumentSource
        val actorFieldArg = actorField.getArgument(remoteArgDef.name)
        val isBatchHydration = actorField.type.unwrapNonNull().isList
        return when (remoteArgSource.sourceType) {
            ObjectField -> {
                val field = (parent.underlying as GraphQLFieldsContainer).getFieldAt(remoteArgSource.pathToField!!)
                if (field == null) {
                    MissingHydrationFieldValueSource(parent, overallField, remoteArgSource)
                } else {
                    // check the input types match with hydration and actor fields
                    return validateHydrationInputArg(
                            field.type,
                            actorFieldArg.type,
                            parent,
                            overallField,
                            remoteArgDef,
                            hydration,
                            isBatchHydration
                    )
                }
            }

            FieldArgument -> {
                val argument = overallField.getArgument(remoteArgSource.argumentName!!)
                if (argument == null) {
                    MissingHydrationArgumentValueSource(parent, overallField, remoteArgSource)
                } else {
                    //check the input types match with hydration and actor fields
                    val hydrationArgType = argument.type
                    return validateHydrationInputArg(
                            hydrationArgType,
                            actorFieldArg.type,
                            parent,
                            overallField,
                            remoteArgDef,
                            hydration,
                            isBatchHydration
                    )
                }
            }

            StaticArgument -> {
                val staticArg = remoteArgSource.staticValue
                if (!validationUtil.isValidLiteralValue(
                                staticArg,
                                actorFieldArg.type,
                                overallSchema,
                                GraphQLContext.getDefault(),
                                Locale.getDefault()
                        )) {
                    return NadelSchemaValidationError.StaticArgIsNotAssignable(
                            parent,
                            overallField,
                            remoteArgDef,
                            actorFieldArg.type,
                    )
                }
                return null
            }
        }
    }
}
