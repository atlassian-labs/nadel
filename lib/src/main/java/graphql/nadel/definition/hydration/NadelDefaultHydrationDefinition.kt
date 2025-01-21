package graphql.nadel.definition.hydration

import graphql.language.DirectiveDefinition
import graphql.nadel.definition.NadelInstructionDefinition
import graphql.nadel.definition.hydration.NadelDefaultHydrationDefinition.Keyword
import graphql.nadel.engine.util.parseDefinition
import graphql.schema.GraphQLAppliedDirective
import graphql.schema.GraphQLDirectiveContainer
import graphql.schema.GraphQLNamedType

fun GraphQLNamedType.hasDefaultHydration(): Boolean {
    return (this as? GraphQLDirectiveContainer)?.hasAppliedDirective(Keyword.defaultHydration) == true
}

fun GraphQLNamedType.parseDefaultHydrationOrNull(): NadelDefaultHydrationDefinition? {
    return (this as? GraphQLDirectiveContainer)?.getAppliedDirective(Keyword.defaultHydration)
        ?.let(::NadelDefaultHydrationDefinition)
}

class NadelDefaultHydrationDefinition(
    private val appliedDirective: GraphQLAppliedDirective,
) : NadelInstructionDefinition {
    companion object {
        val directiveDefinition = parseDefinition<DirectiveDefinition>(
            // language=GraphQL
            """
                "This allows you to hydrate new values into fields"
                directive @defaultHydration(
                    "The backing level field for the data"
                    field: String!
                    "Name of the ID argument on the backing field"
                    idArgument: String!
                    "How to identify matching results"
                    identifiedBy: String! = "id"
                    "The batch size"
                    batchSize: Int! = 200
                    "The timeout to use when completing hydration"
                    timeout: Int! = -1
                ) on OBJECT | INTERFACE
            """.trimIndent(),
        )
    }

    val backingField: List<String>
        get() = appliedDirective.getArgument(Keyword.field).getValue<String>().split(".")

    val identifiedBy: String?
        get() = appliedDirective.getArgument(Keyword.identifiedBy).getValue()

    val idArgument: String
        get() = appliedDirective.getArgument(Keyword.idArgument).getValue()

    val batchSize: Int
        get() = appliedDirective.getArgument(Keyword.batchSize).getValue()

    val timeout: Int
        get() = appliedDirective.getArgument(Keyword.timeout).getValue()

    internal object Keyword {
        const val defaultHydration = "defaultHydration"
        const val field = "field"
        const val identifiedBy = "identifiedBy"
        const val batchSize = "batchSize"
        const val timeout = "timeout"
        const val idArgument = "idArgument"
    }
}
