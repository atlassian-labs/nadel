package graphql.nadel.definition.stubbed

import graphql.language.DirectivesContainer
import graphql.nadel.definition.NadelInstructionDefinition
import graphql.nadel.definition.stubbed.NadelStubbedDefinition.Keyword
import graphql.schema.GraphQLAppliedDirective
import graphql.schema.GraphQLDirectiveContainer
import graphql.schema.GraphQLFieldDefinition

class NadelStubbedDefinition(
    private val appliedDirective: GraphQLAppliedDirective,
) : NadelInstructionDefinition {
    object Keyword {
        const val stubbed = "stubbed"
    }
}

fun GraphQLDirectiveContainer.hasStubbedDefinition(): Boolean {
    return hasAppliedDirective(Keyword.stubbed)
}

fun DirectivesContainer<*>.hasStubbedDefinition(): Boolean {
    return hasDirective(Keyword.stubbed)
}

fun GraphQLFieldDefinition.parseStubbedOrNull(): NadelStubbedDefinition? {
    val directive = getAppliedDirective(Keyword.stubbed)
        ?: return null

    return NadelStubbedDefinition(directive)
}
