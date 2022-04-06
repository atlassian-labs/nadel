package graphql.nadel.validation

import graphql.nadel.dsl.RemoteArgumentDefinition
import graphql.nadel.dsl.RemoteArgumentSource.SourceType.FieldArgument
import graphql.nadel.dsl.RemoteArgumentSource.SourceType.ObjectField
import graphql.nadel.dsl.UnderlyingServiceHydration
import graphql.nadel.engine.util.getFieldAt
import graphql.nadel.engine.util.isList
import graphql.nadel.engine.util.isNonNull
import graphql.nadel.engine.util.isNotWrapped
import graphql.nadel.engine.util.isWrapped
import graphql.nadel.engine.util.unwrapNonNull
import graphql.nadel.engine.util.unwrapOne
import graphql.nadel.validation.NadelSchemaValidationError.DuplicatedHydrationArgument
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleHydrationArgumentType
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleHydrationInputObjectArgumentType
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationArgumentValueSource
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationFieldValueSource
import graphql.nadel.validation.NadelSchemaValidationError.MissingHydrationInputObjectArgumentValueSource
import graphql.nadel.validation.NadelSchemaValidationError.MissingRequiredHydrationActorFieldArgument
import graphql.nadel.validation.NadelSchemaValidationError.NonExistentHydrationActorFieldArgument
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLType

internal class NadelHydrationArgumentValidation(
    private val typeValidation: NadelTypeValidation,
) {
    fun validate(
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

        val remoteArgErrors = mutableListOf<NadelSchemaValidationError>()
        hydration.arguments.mapNotNull { remoteArg ->
            val actorFieldArgument = actorField.getArgument(remoteArg.name)
            if (actorFieldArgument == null) {
                remoteArgErrors.add(NonExistentHydrationActorFieldArgument(
                    parent,
                    overallField,
                    hydration,
                    argument = remoteArg.name,
                ))
            } else {
                remoteArgErrors.addAll(getRemoteArgErrors(parent, overallField, remoteArg, actorField))
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
    ): List<NadelSchemaValidationError> {
        val remoteArgSource = remoteArg.remoteArgumentSource
        val actorArg = actorField.getArgument(remoteArg.name)
        val actorArgInputType = actorArg.type
        return when (remoteArgSource.sourceType) {
            ObjectField -> {
                val field = (parent.underlying as GraphQLFieldsContainer).getFieldAt(remoteArgSource.pathToField!!)
                if (field == null) {
                    listOf(
                        MissingHydrationFieldValueSource(parent, overallField, remoteArgSource)
                    )
                } else {
                    //check the input types match with hydration and actor fields
                    val fieldOutputType = field.type
                    return hydrationArgTypesMatch(parent, overallField, remoteArg, fieldOutputType, actorArgInputType)
                }
            }
            FieldArgument -> {
                val argument = overallField.getArgument(remoteArgSource.argumentName!!)
                if (argument == null) {
                    listOf(MissingHydrationArgumentValueSource(parent, overallField, remoteArgSource))
                } else {
                    //check the input types match with hydration and actor fields
                    val hydrationArgType = argument.type
                    return hydrationArgTypesMatch(parent, overallField, remoteArg, hydrationArgType, actorArgInputType)
                }
            }
            else -> {
                emptyList()
            }
        }
    }

    /*
    *  Checks the type of a hydration argument derived from either a $source field output type or $argument field input
    * argument type against the type of an actor field argument to see if they match
    * will also allow for cases of batched hydration eg ID! -> [ID]
    */
    private fun hydrationArgTypesMatch(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        remoteArg: RemoteArgumentDefinition,
        hydrationSourceType: GraphQLType,
        actorArgumentInputType: GraphQLType,
    ): List<NadelSchemaValidationError> {

        var isIncompatible = false

        var hydrationSource: GraphQLType = hydrationSourceType
        var actorArgument: GraphQLType = actorArgumentInputType

        while (hydrationSource.isWrapped && actorArgument.isWrapped) {
            if (hydrationSource.isNonNull && !actorArgument.isNonNull) {
                //the source can be stricter than the actor input argument
                hydrationSource = hydrationSource.unwrapOne()
            } else if ((hydrationSource.isList && actorArgument.isList) || (hydrationSource.isNonNull && actorArgument.isNonNull)) {
                hydrationSource = hydrationSource.unwrapOne()
                actorArgument = actorArgument.unwrapOne()
            } else if (!hydrationSource.isList && actorArgument.isList) {
                //could be a batch hydration eg ID -> [ID]
                actorArgument = actorArgument.unwrapOne()
            } else if (hydrationSource.isList && !actorArgument.isList) {
                //could be a batch hydration with list input eg [ID] -> ID
                hydrationSource = hydrationSource.unwrapOne()
            } else {
                isIncompatible = true
            }
        }

        if (hydrationSource.isNotWrapped && actorArgument.isNotWrapped) {
            return checkScalarOrObjectTypeAssignability(
                parent,
                overallField,
                remoteArg,
                hydrationSource as GraphQLNamedType,
                actorArgument as GraphQLNamedType,
                hydrationSourceType,
                actorArgumentInputType,
            )
        } else if (hydrationSource.isWrapped && actorArgument.isNotWrapped) {
            //actor argument is more strict than the hydration source - this is ok
            if (hydrationSource.isNonNull && hydrationSource.unwrapNonNull().isNotWrapped) {
                return checkScalarOrObjectTypeAssignability(
                    parent,
                    overallField,
                    remoteArg,
                    hydrationSource.unwrapNonNull() as GraphQLNamedType,
                    actorArgument as GraphQLNamedType,
                    hydrationSourceType,
                    actorArgumentInputType,
                )
            } else if (hydrationSource.isList) {
                // this is a case of batch hydration with list input
                if (hydrationSource.unwrapOne().isNotWrapped) {
                    return checkScalarOrObjectTypeAssignability(
                        parent,
                        overallField,
                        remoteArg,
                        hydrationSource.unwrapOne() as GraphQLNamedType,
                        actorArgument as GraphQLNamedType,
                        hydrationSourceType,
                        actorArgumentInputType,
                    )
                } else {
                    isIncompatible = true
                }
            } else if (hydrationSource.isNotWrapped && actorArgument.isList) {
                // this is a case of batch hydration
                if (actorArgument.unwrapOne().isNotWrapped) {
                    return checkScalarOrObjectTypeAssignability(
                        parent,
                        overallField,
                        remoteArg,
                        hydrationSource as GraphQLNamedType,
                        actorArgument.unwrapOne() as GraphQLNamedType,
                        hydrationSourceType,
                        actorArgumentInputType,
                    )
                } else {
                    isIncompatible = true
                }
            } else {
                isIncompatible = true
            }
        }
        if (isIncompatible) {
            return listOf(
                IncompatibleHydrationArgumentType(
                    parent,
                    overallField,
                    remoteArg,
                    hydrationSourceType,
                    actorArgumentInputType,
                )
            )
        } else {
            return emptyList()
        }
    }

    /*
    *  Checks assignability of a hydration type against actor input argument type to see if either the scalars types
    *  match or if the object type and input object type have the same fields
    *
    */
    private fun checkScalarOrObjectTypeAssignability(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        remoteArg: RemoteArgumentDefinition,
        hydrationSourceType: GraphQLType,
        actorArgumentInputType: GraphQLType,
        originalHydrationSourceType: GraphQLType,
        originalActorArgumentInputType: GraphQLType,
    ): List<NadelSchemaValidationError> {

        val isIncompatible: Boolean

        if (hydrationSourceType is GraphQLScalarType && actorArgumentInputType is GraphQLScalarType) {
            isIncompatible = !typeValidation.isAssignableTo(
                lhs = actorArgumentInputType,
                rhs = hydrationSourceType,
            )
        } else if (hydrationSourceType is GraphQLObjectType && actorArgumentInputType is GraphQLInputObjectType) {
            val hydrationFields = hydrationSourceType.fields.associate { it.name to it.type as GraphQLType }
            val actorFields = actorArgumentInputType.fields.associate { it.name to it.type as GraphQLType }
            return checkInputObjectType(
                parent,
                overallField,
                remoteArg,
                hydrationFields,
                actorFields,
            )
        } else if (hydrationSourceType is GraphQLInputObjectType && actorArgumentInputType is GraphQLInputObjectType) {
            val hydrationFields = hydrationSourceType.fields.associate { it.name to it.type as GraphQLType }
            val actorFields = actorArgumentInputType.fields.associate { it.name to it.type as GraphQLType }
            return checkInputObjectType(
                parent,
                overallField,
                remoteArg,
                hydrationFields,
                actorFields,
            )
        } else {
            isIncompatible = true
        }

        if (isIncompatible) {
            return listOf(
                IncompatibleHydrationArgumentType(
                    parent,
                    overallField,
                    remoteArg,
                    originalHydrationSourceType,
                    originalActorArgumentInputType,
                ))
        } else {
            return emptyList()
        }
    }

    private fun checkInputObjectType(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        remoteArg: RemoteArgumentDefinition,
        hydrationFields: Map<String, GraphQLType>,
        actorInputFields: Map<String, GraphQLType>,
    ): List<NadelSchemaValidationError> {
        val errors = mutableListOf<NadelSchemaValidationError>()
        actorInputFields.forEach { actorFieldName, actorFieldType ->
            val hydrationType = hydrationFields.get(actorFieldName)
            if (hydrationType != null) {
                if (!typesAreSame(hydrationType, actorFieldType)) {
                    errors.add(IncompatibleHydrationInputObjectArgumentType(
                        parent,
                        overallField,
                        remoteArg,
                        actorFieldName,
                        actorFieldType,
                        hydrationType,
                    ))
                }
            } else {
                errors.add(MissingHydrationInputObjectArgumentValueSource(
                    parent,
                    overallField,
                    remoteArg,
                    actorFieldName,
                ))
            }
        }
        return errors
    }

    private fun typesAreSame(hydrationType: GraphQLType, actorFieldType: GraphQLType): Boolean {
        var hydrationSource: GraphQLType = hydrationType
        var actorArgument: GraphQLType = actorFieldType

        while (hydrationSource.isWrapped && actorArgument.isWrapped) {
            if ((hydrationSource.isList && actorArgument.isList) || (hydrationSource.isNonNull && actorArgument.isNonNull)) {
                hydrationSource = hydrationSource.unwrapOne()
                actorArgument = actorArgument.unwrapOne()
            } else {
                return false
            }
        }

        if (hydrationSource.isNotWrapped && actorArgument.isNotWrapped) {
            return typeValidation.isAssignableTo(lhs = actorArgument as GraphQLNamedType,
                rhs = hydrationSource as GraphQLNamedType)
        }
        return false
    }
}
