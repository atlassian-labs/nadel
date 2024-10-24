package graphql.nadel.validation

import graphql.Scalars
import graphql.nadel.definition.hydration.NadelHydrationArgumentDefinition
import graphql.nadel.definition.hydration.NadelHydrationDefinition
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
        backingFieldArgType: GraphQLInputType,
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        remoteArg: NadelHydrationArgumentDefinition,
        hydration: NadelHydrationDefinition,
        isBatchHydration: Boolean,
        backingFieldName: String,
    ): NadelSchemaValidationError? {
        val unwrappedHydrationSourceType = hydrationSourceType.unwrapNonNull()
        val unwrappedBackingFieldArgType = backingFieldArgType.unwrapNonNull()

        //could have ID feed into [ID] (as a possible batch hydration case).
        // in this case we need to unwrap the list and check types
        return if (isBatchHydration && (!unwrappedHydrationSourceType.isList && unwrappedBackingFieldArgType.isList)) {
            val error = getHydrationInputErrors(
                unwrappedHydrationSourceType,
                unwrappedBackingFieldArgType.unwrapOne(),
                parent,
                overallField,
                remoteArg,
                hydration,
                backingFieldName
            )

            if (error != null) {
                NadelSchemaValidationError.IncompatibleHydrationArgumentType(
                    parent,
                    overallField,
                    remoteArg,
                    hydrationSourceType,
                    backingFieldArgType,
                    backingFieldName
                )
            } else {
                null
            }
        }
        //could have [ID] feed into ID (non-batch, ManyToOne case explained in NadelHydrationStrategy.kt)
        // in this case we need to unwrap the list and check types
        else if (!isBatchHydration && (unwrappedHydrationSourceType.isList && !unwrappedBackingFieldArgType.isList)) {
            val error = getHydrationInputErrors(
                unwrappedHydrationSourceType.unwrapOne(),
                unwrappedBackingFieldArgType,
                parent,
                overallField,
                remoteArg,
                hydration,
                backingFieldName
            )
            if (error != null) {
                NadelSchemaValidationError.IncompatibleHydrationArgumentType(
                    parent,
                    overallField,
                    remoteArg,
                    hydrationSourceType,
                    backingFieldArgType,
                    backingFieldName
                )
            } else {
                null
            }
        }
        // Otherwise we can just check the types normally
        else {
            return getHydrationInputErrors(
                hydrationSourceType,
                backingFieldArgType,
                parent,
                overallField,
                remoteArg,
                hydration,
                backingFieldName
            )
        }
    }

    private fun getHydrationInputErrors(
        hydrationSourceType: GraphQLType,
        backingFieldArgType: GraphQLType,
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        remoteArg: NadelHydrationArgumentDefinition,
        hydration: NadelHydrationDefinition,
        backingFieldName: String,
    ): NadelSchemaValidationError? {
        // need to check null compatibility
        if (remoteArg !is NadelHydrationArgumentDefinition.ObjectField && backingFieldArgType.isNonNull && !hydrationSourceType.isNonNull) {
            // source must be at least as strict as field argument
            return NadelSchemaValidationError.IncompatibleHydrationArgumentType(
                parent,
                overallField,
                remoteArg,
                hydrationSourceType,
                backingFieldArgType,
                backingFieldName
            )
        }
        val unwrappedHydrationSourceType = hydrationSourceType.unwrapNonNull()
        val unwrappedBackingFieldArgType = backingFieldArgType.unwrapNonNull()
        // scalar feed into scalar
        return if (unwrappedHydrationSourceType is GraphQLScalarType && unwrappedBackingFieldArgType is GraphQLScalarType) {
            validateScalarArg(
                hydrationSourceType,
                backingFieldArgType,
                parent,
                overallField,
                remoteArg,
                backingFieldName
            )
        }
        // list feed into list
        else if (unwrappedHydrationSourceType.isList && unwrappedBackingFieldArgType.isList) {
            validateListArg(
                hydrationSourceType,
                backingFieldArgType,
                parent,
                overallField,
                remoteArg,
                hydration,
                backingFieldName
            )
        }
        // object feed into inputObject (i.e. hydrating with a $source object)
        else if (remoteArg is NadelHydrationArgumentDefinition.ObjectField && unwrappedHydrationSourceType is GraphQLObjectType && unwrappedBackingFieldArgType is GraphQLInputObjectType) {
            validateInputObjectArg(
                unwrappedHydrationSourceType,
                unwrappedBackingFieldArgType,
                parent,
                overallField,
                remoteArg,
                hydration,
                backingFieldName
            )
        }
        // inputObject feed into inputObject (i.e. hydrating with an $argument object)
        else if (remoteArg is NadelHydrationArgumentDefinition.FieldArgument && unwrappedHydrationSourceType is GraphQLInputObjectType && unwrappedBackingFieldArgType is GraphQLInputObjectType) {
            if (unwrappedHydrationSourceType.name != unwrappedBackingFieldArgType.name) {
                NadelSchemaValidationError.IncompatibleHydrationArgumentType(
                    parent,
                    overallField,
                    remoteArg,
                    hydrationSourceType,
                    backingFieldArgType,
                    backingFieldName
                )
            } else {
                null
            }
        }
        // if enums they must be the same
        else if (unwrappedHydrationSourceType is GraphQLEnumType && unwrappedBackingFieldArgType is GraphQLEnumType) {
            if (unwrappedHydrationSourceType.name != unwrappedBackingFieldArgType.name) {
                NadelSchemaValidationError.IncompatibleHydrationArgumentType(
                    parent,
                    overallField,
                    remoteArg,
                    hydrationSourceType,
                    backingFieldArgType,
                    backingFieldName
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
                backingFieldArgType,
                backingFieldName
            )
        }
    }

    private fun validateScalarArg(
        hydrationSourceType: GraphQLType,
        backingFieldArgType: GraphQLType,
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        remoteArg: NadelHydrationArgumentDefinition,
        backingFieldName: String,
    ): NadelSchemaValidationError? {
        val unwrappedHydrationSourceType = hydrationSourceType.unwrapNonNull()
        val unwrappedBackingFieldArgType = backingFieldArgType.unwrapNonNull()

        return if (unwrappedHydrationSourceType is GraphQLScalarType && unwrappedBackingFieldArgType is GraphQLScalarType) {
            if (isScalarAssignable(unwrappedHydrationSourceType, unwrappedBackingFieldArgType)) {
                null
            } else {
                NadelSchemaValidationError.IncompatibleHydrationArgumentType(
                    parent,
                    overallField,
                    remoteArg,
                    hydrationSourceType,
                    backingFieldArgType,
                    backingFieldName
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
        backingFieldArgType: GraphQLType,
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        remoteArg: NadelHydrationArgumentDefinition,
        hydration: NadelHydrationDefinition,
        backingFieldName: String,
    ): NadelSchemaValidationError? {
        val hydrationSourceFieldInnerType: GraphQLType = hydrationSourceType.unwrapNonNull().unwrapOne()
        val backingFieldArgInnerType: GraphQLType = backingFieldArgType.unwrapNonNull().unwrapOne()
        val errorExists = getHydrationInputErrors(
            hydrationSourceFieldInnerType,
            backingFieldArgInnerType,
            parent,
            overallField,
            remoteArg,
            hydration,
            backingFieldName
        ) != null
        if (errorExists) {
            return NadelSchemaValidationError.IncompatibleHydrationArgumentType(
                parent,
                overallField,
                remoteArg,
                hydrationSourceType,
                backingFieldArgType,
                backingFieldName
            )
        }
        return null
    }

    private fun validateInputObjectArg(
        hydrationSourceType: GraphQLObjectType,
        backingFieldArgType: GraphQLInputObjectType,
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        remoteArg: NadelHydrationArgumentDefinition,
        hydration: NadelHydrationDefinition,
        backingFieldName: String,
    ): NadelSchemaValidationError? {
        for (backingInnerField in backingFieldArgType.fields) {
            val backingInnerFieldName = backingInnerField.name
            val backingInnerFieldType = backingInnerField.type
            val hydrationType = hydrationSourceType.getField(backingInnerField.name)?.type
            if (hydrationType == null) {
                if (backingInnerFieldType.isNonNull) {
                    return NadelSchemaValidationError.MissingFieldInHydratedInputObject(
                        parent,
                        overallField,
                        remoteArg,
                        backingInnerFieldName,
                        backingFieldName
                    )
                }
            } else {
                val thisFieldHasError = getHydrationInputErrors(
                    hydrationType,
                    backingInnerFieldType,
                    parent,
                    overallField,
                    remoteArg,
                    hydration,
                    backingFieldName
                ) != null
                if (thisFieldHasError) {
                    return NadelSchemaValidationError.IncompatibleFieldInHydratedInputObject(
                        parent,
                        overallField,
                        remoteArg,
                        backingInnerFieldName,
                    )
                }
            }
        }
        return null
    }
}
