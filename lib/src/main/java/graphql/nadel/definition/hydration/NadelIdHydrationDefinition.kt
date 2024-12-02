package graphql.nadel.definition.hydration

import graphql.language.DirectiveDefinition
import graphql.language.FieldDefinition
import graphql.nadel.definition.hydration.NadelIdHydrationDefinition.Keyword
import graphql.nadel.engine.util.parseDefinition
import graphql.schema.GraphQLAppliedDirective
import graphql.schema.GraphQLFieldDefinition

fun FieldDefinition.hasIdHydratedDefinition(): Boolean {
    return hasDirective(Keyword.idHydrated)
}

fun GraphQLFieldDefinition.hasIdHydratedDefinition(): Boolean {
    return hasAppliedDirective(Keyword.idHydrated)
}

fun GraphQLFieldDefinition.parseIdHydrationOrNull(): NadelIdHydrationDefinition? {
    return getAppliedDirective(Keyword.idHydrated)
        ?.let(::NadelIdHydrationDefinition)
}

class NadelIdHydrationDefinition(
    private val appliedDirective: GraphQLAppliedDirective,
) {
    companion object {
        val directiveDefinition = parseDefinition<DirectiveDefinition>(
            // language=GraphQL
            """
                "This allows you to hydrate new values into fields"
                directive @idHydrated(
                    "The field that holds the ID value(s) to hydrate"
                    idField: String!
                    "(Optional override) how to identify matching results"
                    identifiedBy: String = null
                ) on FIELD_DEFINITION
            """.trimIndent(),
        )
    }

    val idField: List<String>
        get() = appliedDirective.getArgument(Keyword.idField).getValue<String>().split(".")

    val identifiedBy: String?
        get() = appliedDirective.getArgument(Keyword.identifiedBy).getValue()

    internal object Keyword {
        const val idHydrated = "idHydrated"
        const val idField = "idField"
        const val identifiedBy = "identifiedBy"
    }
}
