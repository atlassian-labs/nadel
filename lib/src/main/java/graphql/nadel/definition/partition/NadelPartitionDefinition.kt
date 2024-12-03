package graphql.nadel.definition.partition

import graphql.language.DirectiveDefinition
import graphql.language.FieldDefinition
import graphql.nadel.definition.NadelDefinition
import graphql.nadel.definition.partition.NadelPartitionDefinition.Keyword
import graphql.nadel.engine.util.parseDefinition
import graphql.schema.GraphQLAppliedDirective
import graphql.schema.GraphQLFieldDefinition

class NadelPartitionDefinition(
    private val appliedDirective: GraphQLAppliedDirective,
) : NadelDefinition {
    companion object {
        val directiveDefinition = parseDefinition<DirectiveDefinition>(
            // language=GraphQL
            """
                "This allows you to partition a field"
                directive @partition(
                    "The path to the split point"
                    pathToPartitionArg: [String!]!
                ) on FIELD_DEFINITION
            """.trimIndent(),
        )
    }

    val pathToPartitionArg: List<String>
        get() = appliedDirective.getArgument(Keyword.pathToPartitionArg).getValue()

    object Keyword {
        const val partition = "partition"
        const val pathToPartitionArg = "pathToPartitionArg"
    }
}

fun GraphQLFieldDefinition.hasPartitionDefinition(): Boolean {
    return hasAppliedDirective(Keyword.partition)
}

fun FieldDefinition.hasPartitionDefinition(): Boolean {
    return hasDirective(Keyword.partition)
}

fun GraphQLFieldDefinition.parsePartitionOrNull(): NadelPartitionDefinition? {
    val directive = getAppliedDirective(Keyword.partition)
        ?: return null

    return NadelPartitionDefinition(directive)
}
