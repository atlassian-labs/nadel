package graphql.nadel.definition.hydration

import graphql.language.DirectiveDefinition
import graphql.language.FieldDefinition
import graphql.nadel.definition.NadelInstructionDefinition
import graphql.nadel.definition.hydration.NadelMaxBatchSizeDefinition.Keyword
import graphql.nadel.engine.util.parseDefinition
import graphql.schema.GraphQLAppliedDirective
import graphql.schema.GraphQLFieldDefinition

fun FieldDefinition.hasMaxBatchSizeDefinition(): Boolean {
    return hasDirective(Keyword.maxBatchSize)
}

fun GraphQLFieldDefinition.hasMaxBatchSizeDefinition(): Boolean {
    return hasAppliedDirective(Keyword.maxBatchSize)
}

fun GraphQLFieldDefinition.parseMaxBatchSizeOrNull(): NadelMaxBatchSizeDefinition? {
    return getAppliedDirective(Keyword.maxBatchSize)
        ?.let(::NadelMaxBatchSizeDefinition)
}

class NadelMaxBatchSizeDefinition(
    private val appliedDirective: GraphQLAppliedDirective,
) : NadelInstructionDefinition {
    companion object {
        val directiveDefinition = parseDefinition<DirectiveDefinition>(
            // language=GraphQL
            """
              directive @maxBatchSize(size: Int!) on FIELD_DEFINITION
            """.trimIndent(),
        )
    }

    val size: Int
        get() = appliedDirective.getArgument(Keyword.size).getValue()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NadelMaxBatchSizeDefinition

        return size == other.size
    }

    override fun hashCode(): Int {
        return size
    }

    internal object Keyword {
        const val maxBatchSize = "maxBatchSize"
        const val size = "size"
    }
}
