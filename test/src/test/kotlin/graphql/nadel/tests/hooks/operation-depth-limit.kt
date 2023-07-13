package graphql.nadel.tests.hooks

import graphql.nadel.Nadel
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook

@UseHook
class `operation-depth-limit` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return super.makeNadel(builder)
            .maxQueryDepth(10)
    }
}
