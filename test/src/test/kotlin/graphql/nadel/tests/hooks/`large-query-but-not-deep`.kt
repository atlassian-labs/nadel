package graphql.nadel.tests.hooks

import graphql.nadel.Nadel
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook

@UseHook
class `large-query-but-not-deep` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return super.makeNadel(builder)
            .maxQueryDepth(10)
    }
}
