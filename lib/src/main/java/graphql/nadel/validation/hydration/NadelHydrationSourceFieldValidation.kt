package graphql.nadel.validation.hydration

import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.blueprint.hydration.NadelHydrationArgument
import graphql.nadel.engine.blueprint.hydration.NadelHydrationCondition
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.validation.NadelValidationContext
import graphql.nadel.validation.NadelValidationInterimResult
import graphql.nadel.validation.NadelValidationInterimResult.Success.Companion.asInterimSuccess
import graphql.nadel.validation.onError
import graphql.schema.GraphQLObjectType

@Deprecated("To be replaced by NadelHydrationSourceFieldValidation2")
internal class NadelHydrationSourceFieldValidation {
    context(NadelValidationContext)
    fun getSourceFields(
        arguments: List<NadelHydrationArgument>,
        hydrationCondition: NadelHydrationCondition?,
    ): NadelValidationInterimResult<List<NadelQueryPath>> {
        val argumentSourceFields = arguments.getSourceFields()
            .onError { return it }
        val conditionSourceFields = listOfNotNull(hydrationCondition?.fieldPath)

        return (argumentSourceFields + conditionSourceFields)
            .asInterimSuccess()
    }

    context(NadelValidationContext)
    fun getBatchHydrationSourceFields(
        arguments: List<NadelHydrationArgument>,
        matchStrategy: NadelBatchHydrationMatchStrategy,
        hydrationCondition: NadelHydrationCondition?,
    ): NadelValidationInterimResult<List<NadelQueryPath>> {
        val argumentSourceFields = arguments.getSourceFields()
            .onError { return it }
        val matcherSourceFields = matchStrategy.getSourceFields()
        val conditionSourceFields = listOfNotNull(hydrationCondition?.fieldPath)

        return removePrefixPaths(argumentSourceFields + matcherSourceFields + conditionSourceFields)
            .toSet()
            .toList()
            .asInterimSuccess()
    }

    private fun removePrefixPaths(paths: List<NadelQueryPath>): List<NadelQueryPath> {
        // Say we have paths = [
        //     [page]
        //     [page.id]
        //     [page.status]
        // ]
        // (e.g. page was the input and the page.id and page.status are used to match batch objects)
        // then this maps it to [
        //     [page.id]
        //     [page.status]
        // ]
        val sourceFieldsFromArgs = paths
            .filter { path ->
                paths.none { otherPath ->
                    otherPath.size > path.size && otherPath.startsWith(path.segments)
                }
            }

        return sourceFieldsFromArgs
    }

    context(NadelValidationContext)
    private fun List<NadelHydrationArgument>.getSourceFields(): NadelValidationInterimResult<List<NadelQueryPath>> {
        return flatMap { argument ->
            when (argument.valueSource) {
                is NadelHydrationArgument.ValueSource.ArgumentValue -> emptyList()
                is NadelHydrationArgument.ValueSource.FieldResultValue -> getSourceFieldQueryPaths(argument.valueSource)
                    .onError { return it }
                is NadelHydrationArgument.ValueSource.StaticValue -> emptyList()
                is NadelHydrationArgument.ValueSource.RemainingArguments -> emptyList()
            }
        }.asInterimSuccess()
    }

    context(NadelValidationContext)
    private fun getSourceFieldQueryPaths(
        hydrationValueSource: NadelHydrationArgument.ValueSource.FieldResultValue,
    ): NadelValidationInterimResult<List<NadelQueryPath>> {
        val hydrationSourceType = hydrationValueSource.fieldDefinition.type.unwrapAll()

        if (hydrationSourceType is GraphQLObjectType) {
            // When the argument of the hydration backing field is an input type and not a primitive
            // we need to add all the input fields to the source fields
            return hydrationSourceType.fields
                .map { field ->
                    hydrationValueSource.queryPathToField.plus(field.name)
                }
                .asInterimSuccess()
        }

        return listOf(hydrationValueSource.queryPathToField)
            .asInterimSuccess()
    }

    private fun NadelBatchHydrationMatchStrategy.getSourceFields(): List<NadelQueryPath> {
        return when (this) {
            is NadelBatchHydrationMatchStrategy.MatchIndex -> emptyList()
            is NadelBatchHydrationMatchStrategy.MatchObjectIdentifier -> listOf(sourceId)
            is NadelBatchHydrationMatchStrategy.MatchObjectIdentifiers -> objectIds.map { it.sourceId }
        }
    }
}
