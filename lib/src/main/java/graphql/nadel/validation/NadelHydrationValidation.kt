package graphql.nadel.validation

import graphql.Scalars
import graphql.nadel.Service
import graphql.nadel.dsl.RemoteArgumentDefinition
import graphql.nadel.dsl.RemoteArgumentSource.SourceType.FieldArgument
import graphql.nadel.dsl.RemoteArgumentSource.SourceType.ObjectField
import graphql.nadel.dsl.UnderlyingServiceHydration
import graphql.nadel.engine.util.getFieldAt
import graphql.nadel.engine.util.isList
import graphql.nadel.engine.util.isNonNull
import graphql.nadel.engine.util.isNotWrapped
import graphql.nadel.engine.util.isWrapped
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.engine.util.unwrapNonNull
import graphql.nadel.engine.util.unwrapOne
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
import graphql.nadel.validation.util.NadelSchemaUtil
import graphql.nadel.validation.util.NadelSchemaUtil.getHydrations
import graphql.nadel.validation.util.NadelSchemaUtil.hasRename
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLImplementingType
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLNamedOutputType
import graphql.schema.GraphQLNamedType
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
        return hydration.isBatched || /*deprecated*/ actorFieldDef!!.type.unwrapNonNull().isList
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
                NonExistentHydrationActorFieldArgument(
                    parent,
                    overallField,
                    hydration,
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
        val actorArg = actorField.getArgument(remoteArg.name)
        val actorArgInputType = actorArg.type
        return when (remoteArgSource.sourceType) {
            ObjectField -> {
                val field = (parent.underlying as GraphQLFieldsContainer).getFieldAt(remoteArgSource.pathToField!!)
                if (field == null) {
                    MissingHydrationFieldValueSource(parent, overallField, remoteArgSource)
                } else {
                    //check the input types match with hydration and actor fields
                    val fieldOutputType = field.type
                    if (!hydrationArgTypesMatch(fieldOutputType, actorArgInputType)) {
                        return IncompatibleHydrationArgumentType(parent, overallField, remoteArg, fieldOutputType.toString(), actorArgInputType.toString())
                    }
                    null
                }
            }
            FieldArgument -> {
                val argument = overallField.getArgument(remoteArgSource.argumentName!!)
                if (argument == null) {
                    MissingHydrationArgumentValueSource(parent, overallField, remoteArgSource)
                } else {
                    //check the input types match with hydration and actor fields
                    val hydrationArgType = argument.type
                    if (!hydrationArgTypesMatch(hydrationArgType, actorArgInputType)) {
                        return IncompatibleHydrationArgumentType(parent, overallField, remoteArg, hydrationArgType.toString(), actorArgInputType.toString())
                    }
                    null
                }
            }
            else -> {
                null
            }
        }
    }

    /*
    *  Checks the type of a hydration argument derived from either a $source field output type or $argument field input
    * argument type against the type of an actor field argument to see if they match
    *
    */
    private fun hydrationArgTypesMatch(
        hydrationSourceType: GraphQLType,
        actorArgumentInputType: GraphQLType,
    ): Boolean {
        var hydrationSource: GraphQLType = hydrationSourceType
        var actorArgument: GraphQLType = actorArgumentInputType

        while (hydrationSource.isWrapped && actorArgument.isWrapped) {
            if ((hydrationSource.isList && actorArgument.isList) || (hydrationSource.isNonNull && actorArgument.isNonNull)) {
                hydrationSource = hydrationSource.unwrapOne()
                actorArgument = actorArgument.unwrapOne()
            } else {
                return false
            }
        }

        if (hydrationSource.isNotWrapped && actorArgument.isNotWrapped) {
            return typeValidation.isAssignableTo(
                lhs = hydrationSource as GraphQLNamedType,
                rhs = actorArgument as GraphQLNamedType,
            )
        } else {
            return false
        }
    }
}
