package graphql.nadel.tests.hooks

import graphql.nadel.tests.EngineTestHook

class `java-ari-transform` : EngineTestHook {
    override val customTransforms = listOf(
        JavaAriTransform.create(),
    )
}
