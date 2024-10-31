package graphql.nadel.validation

import graphql.nadel.definition.hydration.isHydrated
import graphql.nadel.definition.partition.isPartitioned
import graphql.nadel.engine.util.isList
import graphql.nadel.engine.util.unwrapNonNull
import graphql.nadel.schema.NadelDirectives
import graphql.nadel.util.NamespacedUtil
import graphql.nadel.validation.NadelSchemaValidationError.CannotPartitionHydratedField
import graphql.nadel.validation.NadelSchemaValidationError.InvalidPartitionArgument
import graphql.nadel.validation.NadelSchemaValidationError.PartitionAppliedToFieldWithUnsupportedOutputType
import graphql.nadel.validation.NadelSchemaValidationError.PartitionAppliedToSubscriptionField
import graphql.nadel.validation.NadelSchemaValidationError.PartitionAppliedToUnsupportedField
import graphql.nadel.validation.util.NadelSchemaUtil
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLSchema

internal class NadelPartitionValidation(
    private val overallSchema: GraphQLSchema,
) {
    fun validate(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
    ): List<NadelSchemaValidationError> {
        if (!overallField.isPartitioned()) {
            return emptyList()
        }

        if (overallField.isHydrated()) {
            return listOf(
                CannotPartitionHydratedField(parent, overallField),
            )
        }

        val parentObject = parent.overall as? GraphQLObjectType ?: return emptyList()

        val partitionAppliedOnUnsupportedField =
            conditionalError(PartitionAppliedToUnsupportedField(parent, overallField)) {
                !NadelSchemaUtil.isOperation(parentObject) && !NamespacedUtil.isNamespaceType(
                    parentObject,
                    overallSchema
                )
            }

        val partitionAppliedToSubscriptionField =
            conditionalError(PartitionAppliedToSubscriptionField(parent, overallField)) {
                parentObject.name == "Subscription"
            }

        val unsupportedOutputType = conditionalError(
            PartitionAppliedToFieldWithUnsupportedOutputType(
                parent,
                overallField
            )
        ) {
            !overallField.type.unwrapNonNull().isList && !isMutationPayloadType(overallField.type)
        }

        val invalidPartitionArgument = conditionalError(
            InvalidPartitionArgument(
                parent,
                overallField
            )
        ) {
            val pathToPartitionArg = overallField.getAppliedDirective(NadelDirectives.partitionDirectiveDefinition.name)
                ?.getArgument("pathToPartitionArg")
                ?.getValue<List<String>>()
                ?: return@conditionalError true

            if (pathToPartitionArg.isEmpty()) {
                return@conditionalError true
            }

            val argumentRoot = overallField.getArgument(pathToPartitionArg[0])
                ?: return@conditionalError true

            var currentType = argumentRoot.type.unwrapNonNull()

            // start at 1 because we've already checked the first item
            for (i in 1 until pathToPartitionArg.size) {
                val key = pathToPartitionArg[i]

                val inputType = currentType as? GraphQLInputObjectType
                    ?: return@conditionalError true

                currentType = inputType.getField(key).type.unwrapNonNull()
            }

            !currentType.isList
        }

        return partitionAppliedOnUnsupportedField + partitionAppliedToSubscriptionField + unsupportedOutputType + invalidPartitionArgument
    }

    private fun isMutationPayloadType(type: GraphQLOutputType): Boolean {
        val objectType = type.unwrapNonNull() as? GraphQLObjectType ?: return false

        val containsSuccessField = objectType.fieldDefinitions
            .filter { it.name == "success" }
            .map { it.type.unwrapNonNull() }
            .filterIsInstance<GraphQLScalarType>()
            .filter { it.name == "Boolean" }
            .size == 1

        val allExtraFieldsAreOfTypeList = objectType.fieldDefinitions
            .filter { it.name != "success" }
            .all { it.type.unwrapNonNull().isList }

        return containsSuccessField && allExtraFieldsAreOfTypeList
    }

    private inline fun conditionalError(
        error: NadelSchemaValidationError,
        conditionSupplier: () -> Boolean,
    ): List<NadelSchemaValidationError> {
        return if (conditionSupplier()) {
            listOf(error)
        } else {
            emptyList()
        }
    }
}
