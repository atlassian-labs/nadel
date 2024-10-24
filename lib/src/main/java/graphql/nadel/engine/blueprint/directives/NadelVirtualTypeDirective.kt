package graphql.nadel.engine.blueprint.directives

import graphql.schema.GraphQLDirectiveContainer

internal fun GraphQLDirectiveContainer.isVirtualType(): Boolean {
    return hasAppliedDirective(NadelVirtualTypeDirective.SyntaxConstant.virtualType)
}

internal class NadelVirtualTypeDirective {
    object SyntaxConstant {
        const val virtualType = "virtualType"
    }
}
