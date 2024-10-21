package graphql.nadel.validation

import graphql.Scalars
import graphql.nadel.engine.blueprint.directives.NadelHydrationArgumentDefinition
import graphql.nadel.engine.blueprint.directives.NadelHydrationDefinition
import graphql.nadel.engine.util.isList
import graphql.nadel.engine.util.isNonNull
import graphql.nadel.engine.util.unwrapNonNull
import graphql.nadel.engine.util.unwrapOne
import graphql.scalars.ExtendedScalars
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLNamedInputType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLType

internal class NadelHydrationArgumentValidation {
    fun validateHydrationInputArg(
        hydrationSourceType: GraphQLType,
        actorFieldArgType: GraphQLInputType,
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        remoteArg: NadelHydrationArgumentDefinition,
        hydration: NadelHydrationDefinition,
        isBatchHydration: Boolean,
        actorFieldName: String,
    ): NadelSchemaValidationError? {
        val unwrappedHydrationSourceType = hydrationSourceType.unwrapNonNull()
        val unwrappedActorFieldArgType = actorFieldArgType.unwrapNonNull()

        //could have ID feed into [ID] (as a possible batch hydration case).
        // in this case we need to unwrap the list and check types
        return if (isBatchHydration && (!unwrappedHydrationSourceType.isList && unwrappedActorFieldArgType.isList)) {
            val error = getHydrationInputErrors(
                unwrappedHydrationSourceType,
                unwrappedActorFieldArgType.unwrapOne(),
                parent,
                overallField,
                remoteArg,
                hydration,
                actorFieldName
            )

            if (error != null) {
                NadelSchemaValidationError.IncompatibleHydrationArgumentType(
                    parent,
                    overallField,
                    remoteArg,
                    hydrationSourceType,
                    actorFieldArgType,
                    actorFieldName
                )
            } else {
                null
            }
        }
        //could have [ID] feed into ID (non-batch, ManyToOne case explained in NadelHydrationStrategy.kt)
        // in this case we need to unwrap the list and check types
        else if (!isBatchHydration && (unwrappedHydrationSourceType.isList && !unwrappedActorFieldArgType.isList)) {
            val error = getHydrationInputErrors(
                unwrappedHydrationSourceType.unwrapOne(),
                unwrappedActorFieldArgType,
                parent,
                overallField,
                remoteArg,
                hydration,
                actorFieldName
            )
            if (error != null) {
                NadelSchemaValidationError.IncompatibleHydrationArgumentType(
                    parent,
                    overallField,
                    remoteArg,
                    hydrationSourceType,
                    actorFieldArgType,
                    actorFieldName
                )
            } else {
                null
            }
        }
        // Otherwise we can just check the types normally
        else {
            return getHydrationInputErrors(
                hydrationSourceType,
                actorFieldArgType,
                parent,
                overallField,
                remoteArg,
                hydration,
                actorFieldName
            )
        }
    }

    private fun getHydrationInputErrors(
        hydrationSourceType: GraphQLType,
        actorFieldArgType: GraphQLType,
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        remoteArg: NadelHydrationArgumentDefinition,
        hydration: NadelHydrationDefinition,
        actorFieldName: String,
    ): NadelSchemaValidationError? {
        // need to check null compatibility
        val remoteArgumentSource = remoteArg.value
        if (remoteArgumentSource !is NadelHydrationArgumentDefinition.ValueSource.ObjectField && actorFieldArgType.isNonNull && !hydrationSourceType.isNonNull) {
            // source must be at least as strict as field argument
            return NadelSchemaValidationError.IncompatibleHydrationArgumentType(
                parent,
                overallField,
                remoteArg,
                hydrationSourceType,
                actorFieldArgType,
                actorFieldName
            )
        }
        val unwrappedHydrationSourceType = hydrationSourceType.unwrapNonNull()
        val unwrappedActorFieldArgType = actorFieldArgType.unwrapNonNull()
        // scalar feed into scalar
        return if (unwrappedHydrationSourceType is GraphQLScalarType && unwrappedActorFieldArgType is GraphQLScalarType) {
            validateScalarArg(
                hydrationSourceType,
                actorFieldArgType,
                parent,
                overallField,
                remoteArg,
                actorFieldName
            )
        }
        // list feed into list
        else if (unwrappedHydrationSourceType.isList && unwrappedActorFieldArgType.isList) {
            validateListArg(
                hydrationSourceType,
                actorFieldArgType,
                parent,
                overallField,
                remoteArg,
                hydration,
                actorFieldName
            )
        }
        // object feed into inputObject (i.e. hydrating with a $source object)
        else if (remoteArgumentSource is NadelHydrationArgumentDefinition.ValueSource.ObjectField && unwrappedHydrationSourceType is GraphQLObjectType && unwrappedActorFieldArgType is GraphQLInputObjectType) {
            validateInputObjectArg(
                unwrappedHydrationSourceType,
                unwrappedActorFieldArgType,
                parent,
                overallField,
                remoteArg,
                hydration,
                actorFieldName
            )
        }
        // inputObject feed into inputObject (i.e. hydrating with an $argument object)
        else if (remoteArgumentSource is NadelHydrationArgumentDefinition.ValueSource.FieldArgument && unwrappedHydrationSourceType is GraphQLInputObjectType && unwrappedActorFieldArgType is GraphQLInputObjectType) {
            if (unwrappedHydrationSourceType.name != unwrappedActorFieldArgType.name) {
                NadelSchemaValidationError.IncompatibleHydrationArgumentType(
                    parent,
                    overallField,
                    remoteArg,
                    hydrationSourceType,
                    actorFieldArgType,
                    actorFieldName
                )
            } else {
                null
            }
        }
        // if enums they must be the same
        else if (unwrappedHydrationSourceType is GraphQLEnumType && unwrappedActorFieldArgType is GraphQLEnumType) {
            if (unwrappedHydrationSourceType.name != unwrappedActorFieldArgType.name) {
                NadelSchemaValidationError.IncompatibleHydrationArgumentType(
                    parent,
                    overallField,
                    remoteArg,
                    hydrationSourceType,
                    actorFieldArgType,
                    actorFieldName
                )
            } else {
                null
            }
        }
        // Any other types are not assignable
        else {
            NadelSchemaValidationError.IncompatibleHydrationArgumentType(
                parent,
                overallField,
                remoteArg,
                hydrationSourceType,
                actorFieldArgType,
                actorFieldName
            )
        }
    }

    private fun validateScalarArg(
        hydrationSourceType: GraphQLType,
        actorFieldArgType: GraphQLType,
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        remoteArg: NadelHydrationArgumentDefinition,
        actorFieldName: String,
    ): NadelSchemaValidationError? {
        val unwrappedHydrationSourceType = hydrationSourceType.unwrapNonNull()
        val unwrappedActorFieldArgType = actorFieldArgType.unwrapNonNull()

        return if (unwrappedHydrationSourceType is GraphQLScalarType && unwrappedActorFieldArgType is GraphQLScalarType) {
            if (isScalarAssignable(unwrappedHydrationSourceType, unwrappedActorFieldArgType)) {
                null
            } else {
                NadelSchemaValidationError.IncompatibleHydrationArgumentType(
                    parent,
                    overallField,
                    remoteArg,
                    hydrationSourceType,
                    actorFieldArgType,
                    actorFieldName
                )
            }
        } else {
            null
        }
    }

    private fun isScalarAssignable(typeToAssign: GraphQLNamedInputType, targetType: GraphQLNamedInputType): Boolean {
        if (typeToAssign.name == targetType.name) {
            return true
        }
        // Per the spec, when ID is used as an input type, it accepts both Strings and Ints
        if (targetType.name == Scalars.GraphQLID.name &&
            (typeToAssign.name == Scalars.GraphQLString.name || typeToAssign.name == Scalars.GraphQLInt.name || typeToAssign.name == ExtendedScalars.GraphQLLong.name)
        ) {
            return true
        }
        if (targetType.name == Scalars.GraphQLString.name && typeToAssign.name == Scalars.GraphQLID.name) {
            return true
        }
        return false
    }

    private fun validateListArg(
        hydrationSourceType: GraphQLType,
        actorFieldArgType: GraphQLType,
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        remoteArg: NadelHydrationArgumentDefinition,
        hydration: NadelHydrationDefinition,
        actorFieldName: String,
    ): NadelSchemaValidationError? {
        val hydrationSourceFieldInnerType: GraphQLType = hydrationSourceType.unwrapNonNull().unwrapOne()
        val actorFieldArgInnerType: GraphQLType = actorFieldArgType.unwrapNonNull().unwrapOne()
        val errorExists = getHydrationInputErrors(
            hydrationSourceFieldInnerType,
            actorFieldArgInnerType,
            parent,
            overallField,
            remoteArg,
            hydration,
            actorFieldName
        ) != null
        if (errorExists) {
            return NadelSchemaValidationError.IncompatibleHydrationArgumentType(
                parent,
                overallField,
                remoteArg,
                hydrationSourceType,
                actorFieldArgType,
                actorFieldName
            )
        }
        return null
    }

    private fun validateInputObjectArg(
        hydrationSourceType: GraphQLObjectType,
        actorFieldArgType: GraphQLInputObjectType,
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        remoteArg: NadelHydrationArgumentDefinition,
        hydration: NadelHydrationDefinition,
        actorFieldName: String,
    ): NadelSchemaValidationError? {
        for (actorInnerField in actorFieldArgType.fields) {
            val actorInnerFieldName = actorInnerField.name
            val actorInnerFieldType = actorInnerField.type
            val hydrationType = hydrationSourceType.getField(actorInnerField.name)?.type
            if (hydrationType == null) {
                if (actorInnerFieldType.isNonNull) {
                    return NadelSchemaValidationError.MissingFieldInHydratedInputObject(
                        parent,
                        overallField,
                        remoteArg,
                        actorInnerFieldName,
                        actorFieldName
                    )
                }
            } else {
                val thisFieldHasError = getHydrationInputErrors(
                    hydrationType,
                    actorInnerFieldType,
                    parent,
                    overallField,
                    remoteArg,
                    hydration,
                    actorFieldName
                ) != null
                if (thisFieldHasError) {
                    return NadelSchemaValidationError.IncompatibleFieldInHydratedInputObject(
                        parent,
                        overallField,
                        remoteArg,
                        actorInnerFieldName,
                    )
                }
            }
        }
        return null
    }
}
