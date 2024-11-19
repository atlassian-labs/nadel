package graphql.nadel.definition.virtualType

import graphql.language.DirectiveDefinition
import graphql.language.DirectivesContainer
import graphql.nadel.engine.util.parseDefinition
import graphql.schema.GraphQLDirectiveContainer
import graphql.schema.GraphQLSchemaElement

internal fun GraphQLSchemaElement.isVirtualType(): Boolean {
    return (this as? GraphQLDirectiveContainer)
        ?.hasAppliedDirective(NadelVirtualTypeDefinition.directiveDefinition.name) == true
}

internal fun DirectivesContainer<*>.isVirtualType(): Boolean {
    return hasDirective(NadelVirtualTypeDefinition.directiveDefinition.name)
}

internal class NadelVirtualTypeDefinition {
    companion object {
        val directiveDefinition = parseDefinition<DirectiveDefinition>(
            // language=GraphQL
            """
              directive @virtualType on OBJECT
            """.trimIndent(),
        )
    }

    object Keyword {
        const val virtualType = "virtualType"
    }
}
