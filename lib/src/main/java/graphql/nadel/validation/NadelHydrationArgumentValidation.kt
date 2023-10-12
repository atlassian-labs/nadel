package graphql.nadel.validation

import graphql.Scalars
import graphql.nadel.dsl.RemoteArgumentDefinition
import graphql.nadel.dsl.RemoteArgumentSource.SourceType.ObjectField
import graphql.nadel.dsl.RemoteArgumentSource.SourceType.FieldArgument
import graphql.nadel.dsl.UnderlyingServiceHydration
import graphql.nadel.engine.util.isList
import graphql.nadel.engine.util.isNonNull
import graphql.nadel.engine.util.unwrapNonNull
import graphql.nadel.engine.util.unwrapOne
import graphql.schema.*

internal class NadelHydrationArgumentValidation private constructor() {
    companion object {
        fun validateHydrationInputArg(
                hydrationSourceFieldType: GraphQLType,
                actorFieldArgType: GraphQLType,
                parent: NadelServiceSchemaElement,
                overallField: GraphQLFieldDefinition,
                remoteArg: RemoteArgumentDefinition,
                hydration: UnderlyingServiceHydration,
                isBatchHydration: Boolean
        ): NadelSchemaValidationError? {
            val unwrappedHydrationSourceFieldType = hydrationSourceFieldType.unwrapNonNull()
            val unwrappedActorFieldArgType = actorFieldArgType.unwrapNonNull()

            //could have ID feed into [ID] (as a possible batch hydration case).
            // in this case we need to unwrap the list and check types
            if (isBatchHydration && (!unwrappedHydrationSourceFieldType.isList && unwrappedActorFieldArgType.isList)) {
                val error = getHydrationInputErrors(
                        unwrappedHydrationSourceFieldType,
                        unwrappedActorFieldArgType.unwrapOne(),
                        parent,
                        overallField,
                        remoteArg,
                        hydration
                )
                if (error != null) {
                    return NadelSchemaValidationError.IncompatibleHydrationArgumentType(
                            parent,
                            overallField,
                            remoteArg,
                            hydrationSourceFieldType,
                            actorFieldArgType,
                    )
                }
            }
            //could have [ID] feed into ID (non-batch, ManyToOne case explained in NadelHydrationStrategy.kt)
            // in this case we need to unwrap the list and check types
            else if (!isBatchHydration && (unwrappedHydrationSourceFieldType.isList && !unwrappedActorFieldArgType.isList)) {
                val error = getHydrationInputErrors(
                        unwrappedHydrationSourceFieldType.unwrapOne(),
                        unwrappedActorFieldArgType,
                        parent,
                        overallField,
                        remoteArg,
                        hydration
                )
                if (error != null) {
                    return NadelSchemaValidationError.IncompatibleHydrationArgumentType(
                            parent,
                            overallField,
                            remoteArg,
                            hydrationSourceFieldType,
                            actorFieldArgType,
                    )
                }

            }
            // Otherwise we can just check the types normally
            else {
                return getHydrationInputErrors(
                        hydrationSourceFieldType,
                        actorFieldArgType,
                        parent,
                        overallField,
                        remoteArg,
                        hydration
                )
            }
            return null
        }

        private fun getHydrationInputErrors(
                hydrationSourceFieldType: GraphQLType,
                actorFieldArgType: GraphQLType,
                parent: NadelServiceSchemaElement,
                overallField: GraphQLFieldDefinition,
                remoteArg: RemoteArgumentDefinition,
                hydration: UnderlyingServiceHydration
        ): NadelSchemaValidationError? {

            //need to check null compatibility
            val sourceType = remoteArg.remoteArgumentSource.sourceType
            if (sourceType != ObjectField && actorFieldArgType.isNonNull && !hydrationSourceFieldType.isNonNull) {
                // source must be at least as strict as field argument
                return NadelSchemaValidationError.IncompatibleHydrationArgumentType(
                        parent,
                        overallField,
                        remoteArg,
                        hydrationSourceFieldType,
                        actorFieldArgType,
                )
            }
            val unwrappedHydrationSourceFieldType = hydrationSourceFieldType.unwrapNonNull()
            val unwrappedActorFieldArgType = actorFieldArgType.unwrapNonNull()

            // scalar feed into scalar
            if (unwrappedHydrationSourceFieldType is GraphQLScalarType && unwrappedActorFieldArgType is GraphQLScalarType) {
                return validateScalarArg(hydrationSourceFieldType, actorFieldArgType, parent, overallField, remoteArg)
            }
            // list feed into list
            else if (unwrappedHydrationSourceFieldType.isList && unwrappedActorFieldArgType.isList) {
                return validateListArg(hydrationSourceFieldType, actorFieldArgType, parent, overallField, remoteArg, hydration)
            }
            // object feed into inputObject (i.e. hydrating with a $source object)
            else if (sourceType == ObjectField && unwrappedHydrationSourceFieldType is GraphQLObjectType && unwrappedActorFieldArgType is GraphQLInputObjectType) {
                return validateInputObjectArg(unwrappedHydrationSourceFieldType, unwrappedActorFieldArgType, parent, overallField, remoteArg, hydration)
            }
            // inputObject feed into inputObject (i.e. hydrating with a $argument object)
            else if (sourceType == FieldArgument && unwrappedHydrationSourceFieldType is GraphQLInputObjectType && unwrappedActorFieldArgType is GraphQLInputObjectType) {
                if (unwrappedHydrationSourceFieldType.name != unwrappedActorFieldArgType.name) {
                    return NadelSchemaValidationError.IncompatibleHydrationArgumentType(
                            parent,
                            overallField,
                            remoteArg,
                            hydrationSourceFieldType,
                            actorFieldArgType,
                    )
                }
                return null
            }
            // Any other types are not assignable
            return NadelSchemaValidationError.IncompatibleHydrationArgumentType(
                    parent,
                    overallField,
                    remoteArg,
                    hydrationSourceFieldType,
                    actorFieldArgType,
            )
        }

        private fun validateScalarArg(
                hydrationSourceFieldType: GraphQLType,
                actorFieldArgType: GraphQLType,
                parent: NadelServiceSchemaElement,
                overallField: GraphQLFieldDefinition,
                remoteArg: RemoteArgumentDefinition
        ): NadelSchemaValidationError? {
            if (hydrationSourceFieldType.unwrapNonNull() is GraphQLScalarType && actorFieldArgType.unwrapNonNull() is GraphQLScalarType) {
                if (isScalarAssignable(hydrationSourceFieldType.unwrapNonNull() as GraphQLScalarType, actorFieldArgType.unwrapNonNull() as GraphQLScalarType)) {
                    return null
                } else {
                    return NadelSchemaValidationError.IncompatibleHydrationArgumentType(
                            parent,
                            overallField,
                            remoteArg,
                            hydrationSourceFieldType,
                            actorFieldArgType,
                    )
                }
            }
            return null
        }

        private fun isScalarAssignable(typeToAssign: GraphQLNamedInputType, targetType: GraphQLNamedInputType): Boolean {
            if (typeToAssign.name == targetType.name) {
                return true
            }
            if (targetType.name == Scalars.GraphQLID.name &&
                    (typeToAssign.name == Scalars.GraphQLString.name || typeToAssign.name == Scalars.GraphQLInt.name)) {
                return true
            }
            return false
        }

        private fun validateListArg(
                hydrationSourceFieldType: GraphQLType,
                actorFieldArgType: GraphQLType,
                parent: NadelServiceSchemaElement,
                overallField: GraphQLFieldDefinition,
                remoteArg: RemoteArgumentDefinition,
                hydration: UnderlyingServiceHydration
        ): NadelSchemaValidationError? {
            var hydrationSourceFieldInnerType: GraphQLType = hydrationSourceFieldType.unwrapNonNull().unwrapOne()
            var actorFieldArgInnerType: GraphQLType = actorFieldArgType.unwrapNonNull().unwrapOne()
            val errorExists = getHydrationInputErrors(
                    hydrationSourceFieldInnerType,
                    actorFieldArgInnerType,
                    parent,
                    overallField,
                    remoteArg,
                    hydration
            ) != null
            if (errorExists) {
                return NadelSchemaValidationError.IncompatibleHydrationArgumentType(
                        parent,
                        overallField,
                        remoteArg,
                        hydrationSourceFieldType,
                        actorFieldArgType,
                )
            }
            return null
        }

        private fun validateInputObjectArg(
                hydrationSourceFieldType: GraphQLObjectType,
                actorFieldArgType: GraphQLInputObjectType,
                parent: NadelServiceSchemaElement,
                overallField: GraphQLFieldDefinition,
                remoteArg: RemoteArgumentDefinition,
                hydration: UnderlyingServiceHydration
        ): NadelSchemaValidationError? {
            for (actorInnerField in actorFieldArgType.fields) {
                val actorInnerFieldName = actorInnerField.name
                val actorInnerFieldType = actorInnerField.type
                val hydrationType = hydrationSourceFieldType.getField(actorInnerField.name)?.type
                if (hydrationType == null) {
                    if (actorInnerFieldType.isNonNull) {
                        return NadelSchemaValidationError.MissingFieldInHydratedInputObject(
                                parent,
                                overallField,
                                remoteArg,
                                actorInnerFieldName
                        )
                    }
                } else {
                    val thisFieldHasError = getHydrationInputErrors(
                            hydrationType,
                            actorInnerFieldType,
                            parent,
                            overallField,
                            remoteArg,
                            hydration
                    ) != null
                    if (thisFieldHasError) { // want to return top level type to user
                        return NadelSchemaValidationError.IncompatibleFieldInHydratedInputObject(
                                parent,
                                overallField,
                                remoteArg,
                                hydrationSourceFieldType,
                                actorFieldArgType,
                                actorInnerFieldName
                        )
                    }
                }
            }
            return null
        }
    }
}