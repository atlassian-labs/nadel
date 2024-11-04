package graphql.nadel.definition.hydration

import graphql.language.ArrayValue
import graphql.language.DirectiveDefinition
import graphql.language.FieldDefinition
import graphql.language.ObjectValue
import graphql.nadel.definition.hydration.NadelHydrationDefinition.Keyword
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.parseDefinition
import graphql.schema.GraphQLAppliedDirective
import graphql.schema.GraphQLFieldDefinition

fun FieldDefinition.isHydrated(): Boolean {
    return hasDirective(Keyword.hydrated)
}

fun GraphQLFieldDefinition.isHydrated(): Boolean {
    return hasAppliedDirective(Keyword.hydrated)
}

fun GraphQLFieldDefinition.getHydrationDefinitions(): List<NadelHydrationDefinition> {
    return getAppliedDirectives(Keyword.hydrated)
        .map(::NadelHydrationDefinitionImpl)
}

interface NadelHydrationDefinition {
    companion object {
        val directiveDefinition = parseDefinition<DirectiveDefinition>(
            // language=GraphQL
            """
                "This allows you to hydrate new values into fields"
                directive @hydrated(
                    "The target service"
                    service: String
                    "The target top level field"
                    field: String!
                    "How to identify matching results"
                    identifiedBy: String! = "id"
                    "How to identify matching results"
                    inputIdentifiedBy: [NadelBatchObjectIdentifiedBy!]! = []
                    "Are results indexed"
                    indexed: Boolean! = false
                    "The batch size"
                    batchSize: Int! = 200
                    "The timeout to use when completing hydration"
                    timeout: Int! = -1
                    "The arguments to the hydrated field"
                    arguments: [NadelHydrationArgument!]!
                    "Specify a condition for the hydration to activate"
                    when: NadelHydrationCondition
                ) repeatable on FIELD_DEFINITION
            """.trimIndent(),
        )
    }

    val backingField: List<String>

    val identifiedBy: String?

    val isIndexed: Boolean

    val batchSize: Int

    val arguments: List<NadelHydrationArgumentDefinition>

    val condition: NadelHydrationConditionDefinition?

    val timeout: Int

    val inputIdentifiedBy: List<NadelBatchObjectIdentifiedByDefinition>?

    object Keyword {
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

private class NadelHydrationDefinitionImpl(
    private val appliedDirective: GraphQLAppliedDirective,
) : NadelHydrationDefinition {
    override val backingField: List<String>
        get() = appliedDirective.getArgument(Keyword.field).getValue<String>().split(".")

    override val identifiedBy: String?
        get() = appliedDirective.getArgument(Keyword.identifiedBy).getValue()

    override val isIndexed: Boolean
        get() = appliedDirective.getArgument(Keyword.indexed).getValue()

    override val batchSize: Int
        get() = appliedDirective.getArgument(Keyword.batchSize).getValue()

    override val arguments: List<NadelHydrationArgumentDefinition>
        get() = (appliedDirective.getArgument(Keyword.arguments).argumentValue.value as ArrayValue)
            .values
            .map {
                NadelHydrationArgumentDefinition.from(it as ObjectValue)
            }

    override val condition: NadelHydrationConditionDefinition?
        get() = appliedDirective.getArgument(Keyword.`when`).getValue<JsonMap>()
            ?.let {
                NadelHydrationConditionDefinition.from(it)
            }

    override val timeout: Int
        get() = appliedDirective.getArgument(Keyword.timeout).getValue()

    override val inputIdentifiedBy: List<NadelBatchObjectIdentifiedByDefinition>?
        get() = (appliedDirective.getArgument(Keyword.inputIdentifiedBy).argumentValue.value as ArrayValue?)
            ?.values
            ?.map {
                NadelBatchObjectIdentifiedByDefinition(it as ObjectValue)
            }
}
