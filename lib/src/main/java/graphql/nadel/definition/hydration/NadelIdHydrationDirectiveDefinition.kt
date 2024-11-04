package graphql.nadel.definition.hydration

import graphql.language.DirectiveDefinition
import graphql.language.FieldDefinition
import graphql.nadel.definition.hydration.NadelIdHydrationDirectiveDefinition.Keyword
import graphql.nadel.engine.util.parseDefinition
import graphql.schema.GraphQLAppliedDirective
import graphql.schema.GraphQLFieldDefinition

fun FieldDefinition.isIdHydrated(): Boolean {
    return hasDirective(Keyword.idHydrated)
}

fun GraphQLFieldDefinition.isIdHydrated(): Boolean {
    return hasAppliedDirective(Keyword.idHydrated)
}

fun GraphQLFieldDefinition.getIdHydrationOrNull(): NadelIdHydrationDirectiveDefinition? {
    return getAppliedDirective(Keyword.idHydrated)
        ?.let(::NadelIdHydrationDirectiveDefinition)
}

class NadelIdHydrationDirectiveDefinition(
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
