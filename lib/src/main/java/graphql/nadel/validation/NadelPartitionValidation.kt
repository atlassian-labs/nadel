package graphql.nadel.validation

import graphql.Scalars
import graphql.language.OperationDefinition.Operation
import graphql.nadel.definition.partition.NadelPartitionDefinition
import graphql.nadel.engine.blueprint.NadelPartitionInstruction
import graphql.nadel.engine.util.isList
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.engine.util.unwrapNonNull
import graphql.nadel.validation.NadelSchemaValidationError.CannotPartitionHydratedField
import graphql.nadel.validation.NadelSchemaValidationError.InvalidPartitionArgument
import graphql.nadel.validation.NadelSchemaValidationError.PartitionAppliedToFieldWithUnsupportedOutputType
import graphql.nadel.validation.NadelSchemaValidationError.PartitionAppliedToSubscriptionField
import graphql.nadel.validation.NadelSchemaValidationError.PartitionAppliedToUnsupportedField
import graphql.nadel.validation.util.NadelSchemaUtil.isOperation
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLScalarType

internal class NadelPartitionValidation {
    context(NadelValidationContext)
    fun validate(
        parent: NadelServiceSchemaElement.FieldsContainer,
        overallField: GraphQLFieldDefinition,
    ): NadelSchemaValidationResult {
        val partition = instructionDefinitions.getPartitionedOrNull(parent, overallField)
            ?: return ok()

        if (instructionDefinitions.isHydrated(parent, overallField)) {
            return CannotPartitionHydratedField(parent, overallField)
        }

        val parentObject = parent as? NadelServiceSchemaElement.Object
            ?: return ok()

        if (!isOperation(parentObject.overall) && !namespaceTypeNames.contains(parentObject.overall.name)) {
            return PartitionAppliedToUnsupportedField(parent, overallField)
        }

        if (parentObject.overall.name.equals(Operation.SUBSCRIPTION.name, ignoreCase = true)) {
            return PartitionAppliedToSubscriptionField(parent, overallField)
        }

        if (!overallField.type.unwrapNonNull().isList && !isMutationPayloadType(overallField.type)) {
            return PartitionAppliedToFieldWithUnsupportedOutputType(parent, overallField)
        }

        if (isPartitionArgumentInvalid(overallField, partition)) {
            return InvalidPartitionArgument(parent, overallField)
        }

        return NadelValidatedFieldResult(
            service = parent.service,
            fieldInstruction = NadelPartitionInstruction(
                location = makeFieldCoordinates(parent.overall, overallField),
                pathToPartitionArg = partition.pathToPartitionArg,
            ),
        )
    }

    private fun isPartitionArgumentInvalid(
        overallField: GraphQLFieldDefinition,
        partition: NadelPartitionDefinition,
    ): Boolean {
        val pathToPartitionArg = partition.pathToPartitionArg

        if (pathToPartitionArg.isEmpty()) {
            return true
        }

        val argumentRoot = overallField.getArgument(pathToPartitionArg[0])
            ?: return true

        var currentType = argumentRoot.type.unwrapNonNull()

        // start at 1 because we've already checked the first item
        for (i in 1 until pathToPartitionArg.size) {
            val key = pathToPartitionArg[i]

            val inputType = currentType as? GraphQLInputObjectType
                ?: return true

            currentType = inputType.getField(key).type.unwrapNonNull()
        }

        return !currentType.isList
    }

    private fun isMutationPayloadType(type: GraphQLOutputType): Boolean {
        val objectType = type.unwrapNonNull() as? GraphQLObjectType
            ?: return false

        val containsSuccessField = objectType.fieldDefinitions
            .asSequence()
            .filter { it.name == "success" }
            .map { it.type.unwrapNonNull() }
            .filterIsInstance<GraphQLScalarType>()
            .filter { it.name == Scalars.GraphQLBoolean.name }
            .count() == 1

        val allExtraFieldsAreOfTypeList = objectType.fieldDefinitions
            .asSequence()
            .filter { it.name != "success" }
            .all { it.type.unwrapNonNull().isList }

        return containsSuccessField && allExtraFieldsAreOfTypeList
    }
}
