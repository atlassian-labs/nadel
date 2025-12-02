package graphql.nadel.validation.hydration

import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.blueprint.hydration.NadelHydrationArgument
import graphql.nadel.engine.blueprint.hydration.NadelHydrationCondition
import graphql.nadel.engine.transform.query.NFUtil
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.util.getFieldContainerFor
import graphql.nadel.engine.util.isNonNull
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.validation.NadelValidationContext
import graphql.nadel.validation.NadelValidationInterimResult
import graphql.nadel.validation.NadelValidationInterimResult.Success.Companion.asInterimSuccess
import graphql.nadel.validation.onError
import graphql.nadel.validation.onErrorCast
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLObjectType

/**
 * Forked version of [NadelHydrationSourceFieldValidation] that has new functionality so we can feature flag the code.
 */
internal class NadelHydrationSourceFieldValidation2 {
    context(NadelValidationContext, NadelHydrationValidationContext)
    fun getSourceFields(
        arguments: List<NadelHydrationArgument>,
        hydrationCondition: NadelHydrationCondition?,
    ): NadelValidationInterimResult<List<ExecutableNormalizedField>> {
        val argumentSourceFields = arguments.getSourceFields()
            .onError { return it }
        val conditionSourceFields = listOfNotNull(hydrationCondition?.fieldPath).map { makeLeafField(it) }

        return (argumentSourceFields + conditionSourceFields)
            .dedupSourceFields()
            .asInterimSuccess()
    }

    context(NadelValidationContext, NadelHydrationValidationContext)
    fun getBatchHydrationSourceFields(
        arguments: List<NadelHydrationArgument>,
        matchStrategy: NadelBatchHydrationMatchStrategy,
        hydrationCondition: NadelHydrationCondition?,
    ): NadelValidationInterimResult<List<ExecutableNormalizedField>> {
        val argumentSourceFields = arguments.getSourceFields()
            .onError { return it }
        val conditionSourceFields = listOfNotNull(hydrationCondition?.fieldPath).map { makeLeafField(it) }

        return (argumentSourceFields + conditionSourceFields)
            .dedupSourceFields()
            .asInterimSuccess()
    }

    context(NadelValidationContext, NadelHydrationValidationContext)
    private fun List<NadelHydrationArgument>.getSourceFields(): NadelValidationInterimResult<List<ExecutableNormalizedField>> {
        return mapNotNull { argument ->
            when (argument.valueSource) {
                is NadelHydrationArgument.ValueSource.ArgumentValue -> null
                is NadelHydrationArgument.ValueSource.FieldResultValue ->
                    getSourceFieldQueryPaths(argument, argument.valueSource)
                        .onErrorCast { return it }
                is NadelHydrationArgument.ValueSource.StaticValue -> null
                is NadelHydrationArgument.ValueSource.RemainingArguments -> null
            }
        }.asInterimSuccess()
    }

    context(NadelValidationContext, NadelHydrationValidationContext)
    private fun getSourceFieldQueryPaths(
        argument: NadelHydrationArgument,
        hydrationValueSource: NadelHydrationArgument.ValueSource.FieldResultValue,
    ): NadelValidationInterimResult<ExecutableNormalizedField> {
        val hydrationSourceType = hydrationValueSource.fieldDefinition.type.unwrapAll()

        if (hydrationSourceType is GraphQLObjectType) {
            return createObjectField(argument, hydrationValueSource)
        }

        return makeLeafField(hydrationValueSource.queryPathToField)
            .asInterimSuccess()
    }

    context(NadelValidationContext, NadelHydrationValidationContext)
    private fun createObjectField(
        argument: NadelHydrationArgument,
        hydrationValueSource: NadelHydrationArgument.ValueSource.FieldResultValue,
    ): NadelValidationInterimResult<ExecutableNormalizedField> {
        val parentObjectType =
            parent.underlying.getFieldContainerFor(hydrationValueSource.queryPathToField.segments) as GraphQLObjectType

        // todo: should probably check cardinality here too
        val field = makeObjectField(
            parentObjectType = parentObjectType,
            fieldName = hydrationValueSource.fieldDefinition.name,
            inputObjectType = argument.backingArgumentDef.type.unwrapAll() as GraphQLInputObjectType,
            outputObjectType = hydrationValueSource.fieldDefinition.type.unwrapAll() as GraphQLObjectType,
        )

        return if (hydrationValueSource.queryPathToField.size > 1) {
            NFUtil.createField(
                schema = backingService.underlyingSchema,
                parentType = parent.underlying as GraphQLObjectType,
                queryPathToField = hydrationValueSource.queryPathToField.dropLast(1),
                fieldArguments = emptyMap(),
                fieldChildren = listOf(field),
            )
        } else {
            field
        }.asInterimSuccess()
    }

    private fun makeObjectField(
        parentObjectType: GraphQLObjectType,
        fieldName: String,
        inputObjectType: GraphQLInputObjectType,
        outputObjectType: GraphQLObjectType,
    ): ExecutableNormalizedField {
        val children = inputObjectType.fields
            .mapNotNull { inputField ->
                val equivalentOutputField = outputObjectType.getField(inputField.name)
                if (equivalentOutputField == null) {
                    if (inputField.type.isNonNull) { // i.e. required
                        error("Required input field is missed") // todo: proper error here
                    } else {
                        null
                    }
                } else {
                    val parentObjectType = parentObjectType.getField(fieldName).type.unwrapAll() as GraphQLObjectType
                    if (
                        inputField.type.unwrapAll() is GraphQLInputObjectType
                        || equivalentOutputField.type.unwrapAll() is GraphQLObjectType
                    ) {
                        makeObjectField(
                            parentObjectType = parentObjectType,
                            fieldName = inputField.name,
                            inputObjectType = inputField.type.unwrapAll() as GraphQLInputObjectType,
                            outputObjectType = equivalentOutputField.type.unwrapAll() as GraphQLObjectType,
                        )
                    } else {
                        ExecutableNormalizedField.newNormalizedField()
                            .objectTypeNames(listOf(parentObjectType.name))
                            .fieldName(inputField.name)
                            .build()
                    }
                }
            }

        return ExecutableNormalizedField.newNormalizedField()
            .objectTypeNames(listOf(parentObjectType.name))
            .fieldName(fieldName)
            .children(children)
            .build()
    }

    context(NadelValidationContext, NadelHydrationValidationContext)
    private fun makeLeafField(
        path: NadelQueryPath,
    ): ExecutableNormalizedField {
        // todo: should do some validation here?? e.g. arg is a scalar value, type validation? maybe type validation is done elsewhere already
        return NFUtil.createField(
            schema = parent.service.underlyingSchema,
            parentType = parent.underlying as GraphQLObjectType,
            queryPathToField = path,
            fieldArguments = emptyMap(),
            fieldChildren = emptyList(), // This must be a leaf node
        )
    }
}

private fun List<ExecutableNormalizedField>.dedupSourceFields(): List<ExecutableNormalizedField> {
    return groupBy {
        listOf(it.objectTypeNames, it.resultKey, it.name, it.normalizedArguments.size)
    }.flatMap { (_, fields) ->
        if (fields.all { it.normalizedArguments.isEmpty() }) {
            listOf(fields.first())
        } else {
            fields
        }
    }
}
