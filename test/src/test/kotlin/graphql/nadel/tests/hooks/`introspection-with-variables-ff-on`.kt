package graphql.nadel.tests.hooks

import graphql.nadel.NadelExecutionHints
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook

@UseHook
class `introspection-with-variables-ff-on` : EngineTestHook {
    override fun makeExecutionHints(builder: NadelExecutionHints.Builder): NadelExecutionHints.Builder {
        return super.makeExecutionHints(builder)
            .allDocumentVariablesHint { true }
    }
}
