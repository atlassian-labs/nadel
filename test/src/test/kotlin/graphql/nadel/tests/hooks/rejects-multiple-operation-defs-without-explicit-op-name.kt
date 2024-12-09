package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import kotlin.test.assertTrue

@UseHook
class `rejects-multiple-operation-defs-without-explicit-op-name` : EngineTestHook {
    override fun assertResult(result: ExecutionResult) {
        assertTrue(result.getData<Any?>() == null)
        assertTrue(result.errors.single().message.contains("must provide operation name", ignoreCase = true))
    }
}
