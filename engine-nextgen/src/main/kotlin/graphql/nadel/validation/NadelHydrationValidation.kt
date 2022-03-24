package graphql.nadel.validation

import graphql.nadel.Service
import graphql.nadel.dsl.RemoteArgumentDefinition
import graphql.nadel.dsl.RemoteArgumentSource.SourceType.FieldArgument
import graphql.nadel.dsl.RemoteArgumentSource.SourceType.ObjectField
import graphql.nadel.dsl.UnderlyingServiceHydration
import graphql.nadel.enginekt.util.getFieldAt
import graphql.nadel.enginekt.util.isList
import graphql.nadel.enginekt.util.isNonNull
import graphql.nadel.enginekt.util.isNotWrapped
import graphql.nadel.enginekt.util.isWrapped
import graphql.nadel.enginekt.util.unwrapAll
import graphql.nadel.enginekt.util.unwrapNonNull
import graphql.nadel.enginekt.util.unwrapOne
import graphql.nadel.validation.NadelSchemaValidationError.CannotRenameHydratedField
import graphql.nadel.validation.NadelSchemaValidationError.DuplicatedHydrationArgument
import graphql.nadel.validation.NadelSchemaValidationError.FieldWithPolymorphicHydrationMustReturnAUnion
import graphql.nadel.validation.NadelSchemaValidationError.HydrationFieldMustBeNullable
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleHydrationArgumentType
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationActorField
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationActorService
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationArgumentValueSource
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationFieldValueSource
import graphql.nadel.validation.NadelSchemaValidationError.MissingRequiredHydrationActorFieldArgument
import graphql.nadel.validation.NadelSchemaValidationError.NonExistentHydrationActorFieldArgument
import graphql.nadel.validation.NadelSchemaValidationError.PolymorphicHydrationReturnTypeMismatch
import graphql.nadel.validation.util.NadelSchemaUtil.getHydrations
import graphql.nadel.validation.util.NadelSchemaUtil.getUnderlyingType
import graphql.nadel.validation.util.NadelSchemaUtil.hasRename
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLType
import graphql.schema.GraphQLUnionType
import graphql.schema.GraphQLUnmodifiedType

internal class NadelHydrationValidation(
    private val services: Map<String, Service>,
    private val typeValidation: NadelTypeValidation,
    private val overallSchema: GraphQLSchema,
) {
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

        val isPolymorphicHydration = hydrations.size > 1
        val errors = mutableListOf<NadelSchemaValidationError>()
        for (hydration in hydrations) {
            val actorService = services[hydration.serviceName]
            if (actorService == null) {
                errors.add(MissingHydrationActorService(parent, overallField, hydration))
                continue
            }

            val actorServiceQueryType = actorService.underlyingSchema.queryType
            val actorField = actorServiceQueryType.getFieldAt(hydration.pathToActorField)
            if (actorField == null) {
                errors.add(MissingHydrationActorField(parent, overallField, hydration, actorServiceQueryType))
                continue
            }

            val overallActorField = overallSchema.queryType.getFieldAt(hydration.pathToActorField)
            if (overallActorField == null) {
                errors.add(
                    NadelSchemaValidationError.MissingHydrationActorFieldInOverallSchema(
                        parent,
                        overallField,
                        hydration,
                        overallSchema.queryType
                    )
                )
                continue
            }

            val argumentIssues = getArgumentErrors(parent, overallField, hydration, actorServiceQueryType, actorField)
            val outputTypeIssues =
                getOutputTypeIssues(parent, overallField, actorService, actorField, isPolymorphicHydration)
            errors.addAll(argumentIssues)
            errors.addAll(outputTypeIssues)
        }

        if (isPolymorphicHydration) {
            val (batched, notBatched) = hydrations.partition(::isBatched)
            if (batched.isNotEmpty() && notBatched.isNotEmpty()) {
                errors.add(NadelSchemaValidationError.HydrationsMismatch(parent, overallField))
            }
        }

        return errors
    }

    private fun isBatched(hydration: UnderlyingServiceHydration): Boolean {
        val actorFieldDef = overallSchema.queryType.getFieldAt(hydration.pathToActorField)
        return hydration.isBatched || /*deprecated*/ actorFieldDef!!.type.unwrapNonNull().isList
    }

    private fun getOutputTypeIssues(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        actorService: Service,
        actorField: GraphQLFieldDefinition,
        isPolymorphicHydration: Boolean,
    ): List<NadelSchemaValidationError> {
        // Ensures that the underlying type of the actor field matches with the expected overall output type
        var overallType = overallField.type.unwrapAll()
        if (isPolymorphicHydration) {
            if (overallType is GraphQLUnionType) {
                val possibleObjectTypes = overallType.types
                val actorFieldReturnType = actorField.type.unwrapAll().name
                val overallTypeMatchingActorFieldReturnType = possibleObjectTypes.find {
                    actorFieldReturnType == getUnderlyingType(it, actorService)?.name
                }
                val overallTypeName = overallTypeMatchingActorFieldReturnType?.name
                    ?: return listOf(
                        PolymorphicHydrationReturnTypeMismatch(
                            actorField,
                            actorService,
                            parent,
                            overallField
                        )
                    )
                overallType = overallSchema.getType(overallTypeName) as GraphQLUnmodifiedType
            } else {
                return listOf(FieldWithPolymorphicHydrationMustReturnAUnion(parent, overallField))
            }
        }
        val typeValidation = typeValidation.validate(
            NadelServiceSchemaElement(
                overall = overallType,
                underlying = actorField.type.unwrapAll(),
                service = actorService,
            )
        )

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
        actorServiceQueryType: GraphQLObjectType,
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
                NonExistentHydrationActorFieldArgument(
                    parent,
                    overallField,
                    hydration,
                    actorServiceQueryType,
                    argument = remoteArg.name,
                )
            } else {
                getRemoteArgErrors(parent, overallField, remoteArg, actorField)
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
                        actorServiceQueryType,
                        argument = actorArg.name,
                    )
                } else {
                    null
                }
            }

        return duplicatedArgumentsErrors + remoteArgErrors + missingActorArgErrors
    }

    private fun getRemoteArgErrors(
            parent: NadelServiceSchemaElement,
            overallField: GraphQLFieldDefinition,
            remoteArg: RemoteArgumentDefinition,
            actorField: GraphQLFieldDefinition,
    ): NadelSchemaValidationError? {
        val remoteArgSource = remoteArg.remoteArgumentSource
        return when (remoteArgSource.sourceType) {
            ObjectField -> {
                val field = (parent.underlying as GraphQLFieldsContainer).getFieldAt(remoteArgSource.pathToField!!)
                if (field == null) {
                    MissingHydrationFieldValueSource(parent, overallField, remoteArgSource)
                } else {
                    //check the input types match with hydration and actor fields
                    val actorArg = actorField.getArgument(remoteArg.name)
                    val fieldOutputType = field.type
                    val actorArgInputType = actorArg.type
                    if (!outputTypeMatchesInputType(fieldOutputType, actorArgInputType)) {
                        IncompatibleHydrationArgumentType(parent, overallField, remoteArg, fieldOutputType, actorArgInputType)
                    }
                    null
                }
            }
            FieldArgument -> {
                val argument = overallField.getArgument(remoteArgSource.argumentName!!)
                if (argument == null) {
                    MissingHydrationArgumentValueSource(parent, overallField, remoteArgSource)
                } else {
                    // TODO: check argument type is correct
                    null
                }
            }
            else -> {
                null
            }
        }
    }

    /*
    *  Checks the type of a hydration argument against the type of an actor field argument to see if they match
    *
    */
    private fun hydrationArgTypesMatch(outputType: GraphQLOutputType, inputType: GraphQLInputType): Boolean {
        //TODO
        return true
    }
}
