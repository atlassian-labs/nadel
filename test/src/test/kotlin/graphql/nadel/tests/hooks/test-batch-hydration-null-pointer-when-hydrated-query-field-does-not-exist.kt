package graphql.nadel.tests.hooks

import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook

@UseHook
class `test-batch-hydration-null-pointer-when-hydrated-query-field-does-not-exist` : EngineTestHook {
    override fun assertFailure(throwable: Throwable): Boolean {
        assert(throwable.stackTrace.map { it.methodName }.any { it == "makeHydrationFieldInstruction" })

        return true
    }
}
