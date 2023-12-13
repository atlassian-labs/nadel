package graphql.nadel.tests.hooks

import graphql.nadel.NadelExecutionInput
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook

private interface DeferHook : EngineTestHook {
    override fun makeExecutionInput(builder: NadelExecutionInput.Builder): NadelExecutionInput.Builder {
        return super.makeExecutionInput(builder)
            .transformExecutionHints {
                it
                    .deferSupport { true }
            }
    }
}

@UseHook
class `defer-with-label` : DeferHook

@UseHook
class `defer-no-label` : DeferHook

@UseHook
class `defer-on-hydrated-field` : DeferHook

