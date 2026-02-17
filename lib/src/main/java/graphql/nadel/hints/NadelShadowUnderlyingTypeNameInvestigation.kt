package graphql.nadel.hints

import graphql.nadel.engine.NadelExecutionContext

fun interface NadelShadowUnderlyingTypeNameInvestigation {
    operator fun invoke(context: NadelExecutionContext): Boolean
}
