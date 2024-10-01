package graphql.nadel.engine.transform.partition

import graphql.VisibleForTesting
import graphql.language.ScalarValue
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLInputValueDefinition
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLTypeUtil

class NadelFieldPartition(
    private val fieldPartitionContext: NadelFieldPartitionContext,
    private val graphQLSchema: GraphQLSchema,
    private val partitionKeyExtractor: (
        scalarValue: ScalarValue<*>,
        inputValueDef: GraphQLInputValueDefinition,
        userContext: Any?,
    ) -> String?,
) {
    fun createFieldPartitions(
        field: ExecutableNormalizedField,
    ): Map<String, ExecutableNormalizedField> {
        val partitionInstructions = extractPartitionInstructions(field)

        // TODO: better message
        checkNotNull(partitionInstructions) { "Expected values to be partitioned but got null" }

        val partitionedValues = partitionValues(partitionInstructions)
        val pathToPartitionArg = fieldPartitionContext.pathToPartitionArg

        return partitionedValues.mapValues { (_, value) ->
            val partitionArg = copyInsertingNewValue(partitionInstructions.argumentRoot, pathToPartitionArg, value)
            field.transform { builder ->
                val newArgs = HashMap(field.normalizedArguments)
                newArgs[pathToPartitionArg[0]] = partitionArg
                builder.normalizedArguments(newArgs)
            }
        }
    }

    private fun partitionValues(partitionInstructions: PartitionInstructions): Map<String, List<*>> {
        // TODO: Null check
        return partitionInstructions.values.groupBy { value ->
            val partitionKeys = partitionInstructions.inputValueDefinitions.flatMap { inputValueDefinition ->
                collectPartitionKeysForValue(value, inputValueDefinition)
            }.distinct()

            if (partitionKeys.size > 1) {
                throw NadelCannotPartitionFieldException("Expected only one partition key but got ${partitionKeys.size}")
            }

            partitionKeys[0]
        }
    }

    private fun collectPartitionKeysForValue(
        value: Any?,
        inputValueDefinition: GraphQLInputValueDefinition,
    ): List<String> {

        when (value) {
            is Map<*, *> -> {
                val inputObjectType = GraphQLTypeUtil.unwrapAll(inputValueDefinition.getType())
                if (inputObjectType is GraphQLInputObjectType) {
                    return value.entries.flatMap { entry ->
                        collectPartitionKeysForValue(
                            entry.value,
                            inputObjectType.getField(entry.key.toString()),
                        )
                    }
                } else {
                    // TODO: better message & more idiomatic kotlin
                    throw NadelCannotPartitionFieldException("Expected inputObjectType but got $inputValueDefinition")
                }
            }
            is NormalizedInputValue -> {
                return collectPartitionKeysForValue(value.value, inputValueDefinition)
            }
            is ScalarValue<*> -> {
                val partitionKey = partitionKeyExtractor(value, inputValueDefinition, fieldPartitionContext.userContext)

                return if (partitionKey != null) {
                    listOf(partitionKey)
                } else {
                    emptyList()
                }
            }
            is List<*> -> {
                return value.flatMap { item -> collectPartitionKeysForValue(item, inputValueDefinition) }
            }
        }

        // TODO: better message & more idiomatic Kotlin
        throw NadelCannotPartitionFieldException("Cannot partition value $value of type ${inputValueDefinition.name} ")
    }

    private fun copyInsertingNewValue(
        value: NormalizedInputValue,
        path: List<String>,
        newValues: List<*>?,
    ): NormalizedInputValue {
        if (path.isEmpty()) {
            return value
        }

        if (value.isListLike && path.size == 1) {
            return NormalizedInputValue(value.typeName, newValues)
        }

        // Avoid class cast exception?
        val map = value.value as Map<*, *>

        val newValue = map.mapValues { (_, value) ->
            // Avoid class cast exception?
            val nestedValue = value as NormalizedInputValue

            copyInsertingNewValue(nestedValue, path.subList(1, path.size), newValues)
        }

        return NormalizedInputValue(value.typeName, newValue)
    }

    @VisibleForTesting
    fun extractPartitionInstructions(
        field: ExecutableNormalizedField,
    ): PartitionInstructions? {
        val pathToPartitionArg = fieldPartitionContext.pathToPartitionArg

        if (pathToPartitionArg.isEmpty()) {
            // TODO: msg "cannot partition empty path"
            return null
        }
        // The first item in the path is the argument name
        val argumentRoot = field.getNormalizedArgument(pathToPartitionArg[0])
        var value = argumentRoot

        var inputValueDefinitions: List<GraphQLInputValueDefinition> = field.getFieldDefinitions(graphQLSchema)
            .flatMap { fd: GraphQLFieldDefinition -> fd.arguments }
            .filter { arg: GraphQLArgument -> arg.name == pathToPartitionArg[0] }

        // start at 1 because we've already checked the first item
        for (i in 1 until pathToPartitionArg.size) {
            val key = pathToPartitionArg[i]

            val map = value!!.value as Map<*, *>
            val newValue = map[key] as NormalizedInputValue

            val parent = graphQLSchema.getTypeAs<GraphQLInputObjectType>(value.unwrappedTypeName)
            inputValueDefinitions = listOf(parent.getField(key))
            value = newValue
        }

        return if (value != null && value.value is List<*>) {
            PartitionInstructions(inputValueDefinitions, value.value as List<*>, argumentRoot)
        } else {
            // TODO: msg "cannot partition when type of value is not list"
            null
        }
    }

    data class PartitionInstructions(
        val inputValueDefinitions: List<GraphQLInputValueDefinition>, val values: List<*>,
        val argumentRoot: NormalizedInputValue,
    )
}
