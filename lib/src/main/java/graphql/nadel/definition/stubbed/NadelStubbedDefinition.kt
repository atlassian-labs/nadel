package graphql.nadel.definition.stubbed

import graphql.language.DirectiveDefinition
import graphql.language.DirectivesContainer
import graphql.nadel.definition.NadelInstructionDefinition
import graphql.nadel.definition.stubbed.NadelStubbedDefinition.Keyword
import graphql.nadel.engine.util.parseDefinition
import graphql.schema.GraphQLAppliedDirective
import graphql.schema.GraphQLDirectiveContainer
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLNamedType

class NadelStubbedDefinition(
    private val appliedDirective: GraphQLAppliedDirective,
) : NadelInstructionDefinition {
    companion object {
        val directiveDefinition = parseDefinition<DirectiveDefinition>(
            // language=GraphQL
            """
                ""${'"'}
                Allows you to introduce stubbed fields or types.

                Stubbed fields or fields that return stubbed types _always_ return null.

                Stubbed fields are meant to allow frontend clients consume new schema elements earlier so that they can iterate faster.
                ""${'"'}
                directive @stubbed on FIELD_DEFINITION | OBJECT | INPUT_OBJECT
            """.trimIndent(),
        )
    }

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

fun GraphQLNamedType.parseStubbedOrNull(): NadelStubbedDefinition? {
    val directive = (this as GraphQLDirectiveContainer).getAppliedDirective(Keyword.stubbed)
        ?: return null

    return NadelStubbedDefinition(directive)
}
