package graphql.nadel.validation

import graphql.Scalars
import graphql.nadel.dsl.RemoteArgumentDefinition
import graphql.nadel.dsl.UnderlyingServiceHydration
import graphql.nadel.engine.util.isList
import graphql.nadel.engine.util.isNonNull
import graphql.nadel.engine.util.unwrapNonNull
import graphql.nadel.engine.util.unwrapOne
import graphql.schema.*

internal class NadelHydrationArgumentValidation private constructor() {
    companion object {
        /*
*  Checks the type of a hydration argument derived from either a $source field output type or $argument field input
* argument type against the type of an actor field argument to see if they match
* will also allow for cases of batched hydration eg ID! -> [ID]
*/
        fun getHydrationInputErrors(
                hydrationSourceFieldType: GraphQLType,
                actorFieldArgType: GraphQLType,
                parent: NadelServiceSchemaElement,
                overallField: GraphQLFieldDefinition,
                remoteArg: RemoteArgumentDefinition,
                hydration: UnderlyingServiceHydration
        ): NadelSchemaValidationError? {

            //need to check null compatibility for inner types (as this is recursive)
            if (actorFieldArgType.isNonNull && !hydrationSourceFieldType.isNonNull) {
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

            // check the actual type
            if (unwrappedHydrationSourceFieldType is GraphQLScalarType && unwrappedActorFieldArgType is GraphQLScalarType) {
                return validateScalarArg(hydrationSourceFieldType, actorFieldArgType, parent, overallField, remoteArg, hydration)
            }
            else if (unwrappedHydrationSourceFieldType.isList && unwrappedActorFieldArgType.isList) {
                return validateListArg(hydrationSourceFieldType, actorFieldArgType, parent, overallField, remoteArg, hydration)
            }
            else if (unwrappedHydrationSourceFieldType is GraphQLObjectType && unwrappedActorFieldArgType is GraphQLInputObjectType) {
                return validateInputObjectArg(hydrationSourceFieldType, actorFieldArgType, parent, overallField, remoteArg, hydration)
            }
            else {
                return NadelSchemaValidationError.IncompatibleHydrationArgumentType(
                        parent,
                        overallField,
                        remoteArg,
                        hydrationSourceFieldType,
                        actorFieldArgType,
                )
            }
        }

        private fun validateScalarArg(
                hydrationSourceFieldType: GraphQLType,
                actorFieldArgType: GraphQLType,
                parent: NadelServiceSchemaElement,
                overallField: GraphQLFieldDefinition,
                remoteArg: RemoteArgumentDefinition,
                hydration: UnderlyingServiceHydration
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
            if (targetType is GraphQLInterfaceType && typeToAssign is GraphQLImplementingType) {
                return typeToAssign.interfaces.contains(targetType)
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
                hydrationSourceFieldType: GraphQLType,
                actorFieldArgType: GraphQLType,
                parent: NadelServiceSchemaElement,
                overallField: GraphQLFieldDefinition,
                remoteArg: RemoteArgumentDefinition,
                hydration: UnderlyingServiceHydration
        ): NadelSchemaValidationError? {
            val unwrappedHydrationSourceFieldType = hydrationSourceFieldType.unwrapNonNull() as GraphQLObjectType
            val unwrappedActorFieldArgType = actorFieldArgType.unwrapNonNull() as GraphQLInputObjectType

            val hydrationInnerFields: Map<String, GraphQLType> = unwrappedHydrationSourceFieldType.fields.associate { it.name to it.type as GraphQLType }
            val actorInnerFields: Map<String, GraphQLType> = unwrappedActorFieldArgType.fields.associate { it.name to it.type as GraphQLType }

            for (innerField in actorInnerFields){
                val actorInnerFieldName = innerField.key
                val actorInnerFieldType = innerField.value
                val hydrationType = hydrationInnerFields.get(actorInnerFieldName)
                if (hydrationType == null) {
                    return NadelSchemaValidationError.MissingFieldInHydratedInputObject(
                            parent,
                            overallField,
                            remoteArg,
                            actorInnerFieldName
                    )
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