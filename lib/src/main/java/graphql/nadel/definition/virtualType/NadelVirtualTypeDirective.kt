package graphql.nadel.definition.virtualType

import graphql.language.DirectiveDefinition
import graphql.nadel.engine.util.parseDefinition
import graphql.schema.GraphQLDirectiveContainer

internal fun GraphQLDirectiveContainer.isVirtualType(): Boolean {
    return hasAppliedDirective(NadelVirtualTypeDirective.Keyword.virtualType)
}

internal class NadelVirtualTypeDirective {
    companion object {
        val directiveDefinition = parseDefinition<DirectiveDefinition>(
            """
                directive @virtualType on OBJECT
            """.trimIndent(),
        )
    }

    object Keyword {
        const val virtualType = "virtualType"
    }
}
