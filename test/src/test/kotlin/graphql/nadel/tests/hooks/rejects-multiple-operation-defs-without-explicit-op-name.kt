package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.nadel.tests.NadelEngineType
import strikt.api.expectThat
import strikt.assertions.containsIgnoringCase
import strikt.assertions.isNotEmpty
import strikt.assertions.isNull
import strikt.assertions.single

@UseHook
class `rejects-multiple-operation-defs-without-explicit-op-name` : EngineTestHook {
    override fun assertResult(engineType: NadelEngineType, result: ExecutionResult) {
        expectThat(result.getData<Any?>())
            .isNull()
        expectThat(result.errors)
            .isNotEmpty()
            .single()
            .get { message }
            .containsIgnoringCase("must provide operation name")
    }
}
