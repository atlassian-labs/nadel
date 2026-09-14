package graphql.nadel.tests.hooks

import graphql.introspection.GoodFaithIntrospection
import graphql.nadel.NadelExecutionInput
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook

@UseHook
class `introspection-does-not-show-hidden-fields` : EngineTestHook {
    override fun makeExecutionInput(builder: NadelExecutionInput.Builder): NadelExecutionInput.Builder {
        // This fixture checks visibility by querying __Type.fields for two different types.
        return builder.graphqlContext(GoodFaithIntrospection.GOOD_FAITH_INTROSPECTION_DISABLED, true)
    }
}
