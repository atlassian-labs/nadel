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
import graphql.schema.GraphQLList
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLType

internal class NadelHydrationArgumentValidation {
    private data class ValidationContext(
        val parent: NadelServiceSchemaElement,
        val overallField: GraphQLFieldDefinition,
        val remoteArg: NadelHydrationArgumentDefinition,
        val hydration: NadelHydrationDefinition,
        val isBatchHydration: Boolean,
        val backingFieldName: String,
    )

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
        val context = ValidationContext(
            parent = parent,
            overallField = overallField,
            remoteArg = remoteArg,
            hydration = hydration,
            isBatchHydration = isBatchHydration,
            backingFieldName = backingFieldName,
        )

        return with(context) {
            validateHydrationInputArg(hydrationSourceType, backingFieldArgType)
        }
    }

    context(ValidationContext)
    private fun validateHydrationInputArg(
        hydrationSourceType: GraphQLType,
        backingFieldArgType: GraphQLInputType,
    ): NadelSchemaValidationError? {
        val unwrappedHydrationSourceType = hydrationSourceType.unwrapNonNull()
        val unwrappedBackingFieldArgType = backingFieldArgType.unwrapNonNull()

        //could have ID feed into [ID] (as a possible batch hydration case).
        // in this case we need to unwrap the list and check types
        return if (isBatchHydration && (!unwrappedHydrationSourceType.isList && unwrappedBackingFieldArgType.isList)) {
            val error = getHydrationInputError(
                unwrappedHydrationSourceType,
                unwrappedBackingFieldArgType.unwrapOne()
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
            val error = getHydrationInputError(
                unwrappedHydrationSourceType.unwrapOne(),
                unwrappedBackingFieldArgType
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
            return getHydrationInputError(
                hydrationSourceType,
                backingFieldArgType
            )
        }
    }

    context(ValidationContext)
    private fun getHydrationInputError(
        hydrationSourceType: GraphQLType,
        backingFieldArgType: GraphQLType,
    ): NadelSchemaValidationError? {
        // need to check null compatibility
        val remoteArgumentSource = remoteArg.value
        if (remoteArgumentSource !is NadelHydrationArgumentDefinition.ValueSource.ObjectField && backingFieldArgType.isNonNull && !hydrationSourceType.isNonNull) {
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
                unwrappedHydrationSourceType,
                unwrappedBackingFieldArgType,
            )
        }
        // list feed into list
        else if (unwrappedHydrationSourceType is GraphQLList && unwrappedBackingFieldArgType is GraphQLList) {
            validateListArg(
                unwrappedHydrationSourceType,
                unwrappedBackingFieldArgType
            )
        }
        // object feed into inputObject (i.e. hydrating with a $source object)
        else if (remoteArgumentSource is NadelHydrationArgumentDefinition.ValueSource.ObjectField && unwrappedHydrationSourceType is GraphQLObjectType && unwrappedBackingFieldArgType is GraphQLInputObjectType) {
            validateInputObjectArg(
                unwrappedHydrationSourceType,
                unwrappedBackingFieldArgType
            )
        }
        // inputObject feed into inputObject (i.e. hydrating with an $argument object)
        else if (remoteArgumentSource is NadelHydrationArgumentDefinition.ValueSource.FieldArgument && unwrappedHydrationSourceType is GraphQLInputObjectType && unwrappedBackingFieldArgType is GraphQLInputObjectType) {
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

    context(ValidationContext)
    private fun validateScalarArg(
        hydrationSourceType: GraphQLScalarType,
        backingFieldArgType: GraphQLScalarType,
    ): NadelSchemaValidationError? {
        return if (isScalarAssignable(hydrationSourceType, backingFieldArgType)) {
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
    }

    context(ValidationContext)
    private fun isScalarAssignable(typeToAssign: GraphQLScalarType, targetType: GraphQLScalarType): Boolean {
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

    context(ValidationContext)
    private fun validateListArg(
        hydrationSourceType: GraphQLList,
        backingFieldArgType: GraphQLList,
    ): NadelSchemaValidationError? {
        val hydrationSourceFieldInnerType: GraphQLType = hydrationSourceType.unwrapOne()
        val backingFieldArgInnerType: GraphQLType = backingFieldArgType.unwrapOne()

        val errorExists = getHydrationInputError(
            hydrationSourceFieldInnerType,
            backingFieldArgInnerType
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

    context(ValidationContext)
    private fun validateInputObjectArg(
        hydrationSourceType: GraphQLObjectType,
        backingFieldArgType: GraphQLInputObjectType,
    ): NadelSchemaValidationError? {
        for (backingField in backingFieldArgType.fields) {
            val backingFieldName = backingField.name
            val backingFieldType = backingField.type
            val hydrationType = hydrationSourceType.getField(backingField.name)?.type
            if (hydrationType == null) {
                if (backingFieldType.isNonNull) {
                    return NadelSchemaValidationError.MissingFieldInHydratedInputObject(
                        parent,
                        overallField,
                        remoteArg,
                        backingFieldName,
                        backingFieldName
                    )
                }
            } else {
                val thisFieldHasError = getHydrationInputError(
                    hydrationType,
                    backingFieldType
                ) != null
                if (thisFieldHasError) {
                    return NadelSchemaValidationError.IncompatibleFieldInHydratedInputObject(
                        parent,
                        overallField,
                        remoteArg,
                        backingFieldName,
                    )
                }
            }
        }

        return null
    }
}
