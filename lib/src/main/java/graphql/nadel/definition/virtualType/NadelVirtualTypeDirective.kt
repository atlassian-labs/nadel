package graphql.nadel.definition.virtualType

import graphql.schema.GraphQLDirectiveContainer

internal fun GraphQLDirectiveContainer.isVirtualType(): Boolean {
    return hasAppliedDirective(NadelVirtualTypeDirective.Keyword.virtualType)
}

internal class NadelVirtualTypeDirective {
    object Keyword {
        const val virtualType = "virtualType"
    }
}
