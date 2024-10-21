package graphql.nadel.engine.blueprint.directives

import graphql.language.ArrayValue
import graphql.language.ObjectField
import graphql.language.ObjectValue
import graphql.language.StringValue
import graphql.nadel.dsl.NadelHydrationConditionDefinition
import graphql.nadel.engine.blueprint.directives.NadelHydrationDefinition.Keyword
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.util.AnyAstValue
import graphql.schema.GraphQLAppliedDirective
import graphql.schema.GraphQLFieldDefinition

internal fun GraphQLFieldDefinition.hasHydration(): Boolean {
    return hasAppliedDirective(Keyword.hydrated)
}

internal fun GraphQLFieldDefinition.getHydrationDefinitions(): List<NadelHydrationDefinition> {
    return getAppliedDirectives(Keyword.hydrated)
        .map(::NadelHydrationDefinition)
}

/**
 * ```graphql
 * "This allows you to hydrate new values into fields"
 * directive @hydrated(
 *     "The target service"
 *     service: String!
 *     "The target top level field"
 *     field: String!
 *     "How to identify matching results"
 *     identifiedBy: String! = "id"
 *     "How to identify matching results"
 *     inputIdentifiedBy: [NadelBatchObjectIdentifiedBy!]! = []
 *     "Are results indexed"
 *     indexed: Boolean! = false
 *     "Is querying batched"
 *     batched: Boolean! = false
 *     "The batch size"
 *     batchSize: Int! = 200
 *     "The timeout to use when completing hydration"
 *     timeout: Int! = -1
 *     "The arguments to the hydrated field"
 *     arguments: [NadelHydrationArgument!]!
 *     "Specify a condition for the hydration to activate"
 *     when: NadelHydrationCondition
 * ) repeatable on FIELD_DEFINITION
 * ```
 */
internal class NadelHydrationDefinition(
    private val appliedDirective: GraphQLAppliedDirective,
) {
    val backingField: List<String>
        get() = appliedDirective.getArgument(Keyword.field).getValue<String>().split(".")

    val identifiedBy: String?
        get() = appliedDirective.getArgument(Keyword.identifiedBy).getValue()

    val isIndexed: Boolean
        get() = appliedDirective.getArgument(Keyword.indexed).getValue()

    val isBatched: Boolean
        get() = appliedDirective.getArgument(Keyword.batched).getValue()

    val batchSize: Int
        get() = appliedDirective.getArgument(Keyword.batchSize).getValue()

    val arguments: List<NadelHydrationArgumentDefinition>
        get() = appliedDirective.getArgument(Keyword.arguments).getValue<ArrayValue>()
            .values
            .map {
                NadelHydrationArgumentDefinition(it as ObjectValue)
            }

    val condition: NadelHydrationConditionDefinition?
        get() = appliedDirective.getArgument(Keyword.`when`).getValue<JsonMap>()
            ?.let {
                NadelHydrationConditionDefinition.from(it)
            }

    val timeout: Int
        get() = appliedDirective.getArgument(Keyword.timeout).getValue()

    val inputIdentifiedBy: List<NadelBatchObjectIdentifiedByDefinition>?
        get() = appliedDirective.getArgument(Keyword.inputIdentifiedBy).getValue<ArrayValue>()
            .values
            .map {
                NadelBatchObjectIdentifiedByDefinition(it as ObjectValue)
            }

    internal object Keyword {
        const val hydrated = "hydrated"
        const val field = "field"
        const val identifiedBy = "identifiedBy"
        const val indexed = "indexed"
        const val batched = "batched"
        const val batchSize = "batchSize"
        const val arguments = "arguments"
        const val timeout = "timeout"
        const val `when` = "when"
        const val inputIdentifiedBy = "inputIdentifiedBy"
    }
}

internal class NadelBatchObjectIdentifiedByDefinition(
    private val objectValue: ObjectValue,
) {
    val sourceId: String
        get() = (objectValue.getObjectField(Keyword.sourceId).value as StringValue).value
    val resultId: String
        get() = (objectValue.getObjectField(Keyword.resultId).value as StringValue).value

    internal object Keyword {
        const val sourceId = "sourceId"
        const val resultId = "resultId"
    }
}

/**
 * Argument belonging to [NadelHydrationDefinition.arguments]
 */
internal class NadelHydrationArgumentDefinition(
    private val argumentObject: ObjectValue,
) {
    /**
     * Name of the backing field's argument.
     */
    val name: String
        get() = (argumentObject.getObjectField(Keyword.name).value as StringValue).value

    /**
     * Value to support to the backing field's argument at runtime.
     */
    val value: ValueSource
        get() = ValueSource.from(argumentObject.getObjectField(Keyword.value).value)

    internal object Keyword {
        const val name = "name"
        const val value = "value"
    }

    internal sealed class ValueSource {
        data class ObjectField(
            val pathToField: List<String>,
        ) : ValueSource()

        data class FieldArgument(
            val argumentName: String,
        ) : ValueSource()

        data class StaticArgument(
            val staticValue: AnyAstValue,
        ) : ValueSource()

        companion object {
            fun from(astValue: AnyAstValue): ValueSource {
                return if (astValue is StringValue && astValue.value.startsWith("$")) {
                    val command = astValue.value.substringBefore(".")
                    val values = astValue.value.substringAfter(".").split('.')

                    when (command) {
                        "\$source" -> ObjectField(
                            pathToField = values,
                        )
                        "\$argument" -> FieldArgument(
                            argumentName = values.single(),
                        )
                        else -> StaticArgument(staticValue = astValue)
                    }
                } else {
                    StaticArgument(staticValue = astValue)
                }
            }
        }
    }
}

internal fun ObjectValue.getObjectField(name: String): ObjectField {
    return objectFields.first { it.name == name }
}
