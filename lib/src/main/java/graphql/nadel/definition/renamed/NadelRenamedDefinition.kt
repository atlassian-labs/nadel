package graphql.nadel.definition.renamed

import graphql.language.DirectiveDefinition
import graphql.language.DirectivesContainer
import graphql.nadel.definition.NadelInstructionDefinition
import graphql.nadel.definition.renamed.NadelRenamedDefinition.Keyword
import graphql.nadel.engine.util.parseDefinition
import graphql.schema.GraphQLAppliedDirective
import graphql.schema.GraphQLDirectiveContainer
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLNamedType

sealed class NadelRenamedDefinition : NadelInstructionDefinition {
    companion object {
        val directiveDefinition = parseDefinition<DirectiveDefinition>(
            // language=GraphQL
            """
                "This allows you to rename a type or field in the overall schema"
                directive @renamed(
                    "The type to be renamed"
                    from: String!
                ) on FIELD_DEFINITION | OBJECT | INTERFACE | UNION | INPUT_OBJECT | SCALAR | ENUM
            """.trimIndent(),
        )
    }

    class Type(
        private val appliedDirective: GraphQLAppliedDirective,
    ) : NadelRenamedDefinition() {
        val from: String
            get() = appliedDirective.getArgument(Keyword.from).getValue()
    }

    class Field(
        private val appliedDirective: GraphQLAppliedDirective,
    ) : NadelRenamedDefinition() {
        val from: List<String>
            get() = rawFrom.split(".")

        val rawFrom: String
            get() = appliedDirective.getArgument(Keyword.from).getValue()
    }

    object Keyword {
        const val renamed = "renamed"
        const val from = "from"
    }
}

fun GraphQLDirectiveContainer.hasRenameDefinition(): Boolean {
    return hasAppliedDirective(Keyword.renamed)
}

fun DirectivesContainer<*>.hasRenameDefinition(): Boolean {
    return hasDirective(Keyword.renamed)
}

fun GraphQLFieldDefinition.parseRenamedOrNull(): NadelRenamedDefinition.Field? {
    val directive = getAppliedDirective(Keyword.renamed)
        ?: return null

    return NadelRenamedDefinition.Field(directive)
}

fun GraphQLNamedType.parseRenamedOrNull(): NadelRenamedDefinition.Type? {
    val directive = (this as GraphQLDirectiveContainer).getAppliedDirective(Keyword.renamed)
        ?: return null

    return NadelRenamedDefinition.Type(directive)
}
