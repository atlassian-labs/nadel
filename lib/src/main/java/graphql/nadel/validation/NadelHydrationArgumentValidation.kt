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
            val isBatchHydration = actorFieldArgType.unwrapNonNull().isList
            if (isBatchHydration) {
                return getBatchInputTypeValidationErrors(hydrationSourceFieldType, actorFieldArgType, parent, overallField, remoteArg, hydration)
            }
            return getInputTypeValidationErrors(hydrationSourceFieldType, actorFieldArgType, parent, overallField, remoteArg, hydration)
        }

        private fun getInputTypeValidationErrors(
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
                    return NadelSchemaValidationError.IncompatibleArgumentTypeForActorField(
                            parent,
                            overallField,
                            hydration,
                            argument = remoteArg.name,
                    )
                }
            }

            return null;
        }

        private fun getBatchInputTypeValidationErrors(
                hydrationSourceFieldType: GraphQLType,
                actorFieldArgType: GraphQLType,
                parent: NadelServiceSchemaElement,
                overallField: GraphQLFieldDefinition,
                remoteArg: RemoteArgumentDefinition,
                hydration: UnderlyingServiceHydration
        ): NadelSchemaValidationError? {

            if (hydrationSourceFieldType.unwrapNonNull().isList != actorFieldArgType.unwrapNonNull().isList) {
                return NadelSchemaValidationError.IncompatibleArgumentTypeForActorField(
                        parent,
                        overallField,
                        hydration,
                        argument = remoteArg.name,
                )
            }

            var hydrationSourceFieldInnerType: GraphQLType = hydrationSourceFieldType.unwrapNonNull().unwrapOne()
            var actorFieldArgInnerType: GraphQLType = actorFieldArgType.unwrapNonNull().unwrapOne()

            //need to check null compatibility for inner type
            if (actorFieldArgInnerType.isNonNull && !hydrationSourceFieldInnerType.isNonNull) {
                // source must be at least as strict as field argument
                return NadelSchemaValidationError.IncompatibleArgumentTypeForActorField(
                        parent,
                        overallField,
                        hydration,
                        argument = remoteArg.name,
                )
            }

            return getHydrationInputErrors(
                    hydrationSourceFieldInnerType,
                    actorFieldArgInnerType,
                    parent,
                    overallField,
                    remoteArg,
                    hydration
            )


        }

        private fun isScalarAssignable(typeToAssign: GraphQLNamedInputType, targetType: GraphQLNamedInputType): Boolean {
            if (typeToAssign.name == targetType.name) {
                return true
            }
            if (targetType.name == Scalars.GraphQLID.name && typeToAssign.name == Scalars.GraphQLString.name) {
                return true
            }
            if (targetType is GraphQLInterfaceType && typeToAssign is GraphQLImplementingType) {
                return typeToAssign.interfaces.contains(targetType)
            }
            return false
        }
    }

}