package graphql.nadel.tests.hooks

import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.nadel.tests.NadelEngineType
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.isEqualTo

@UseHook
class `test-batch-hydration-null-pointer-when-hydrated-query-field-does-not-exist` : EngineTestHook {
    override fun assertFailure(engineType: NadelEngineType, throwable: Throwable): Boolean {
        when (engineType) {
            NadelEngineType.current -> expectThat(throwable)
                .get { message }
                .isEqualTo("hydration field 'doesNotExist' does not exist in underlying schema in service 'Bar'")
            NadelEngineType.nextgen -> expectThat(throwable)
                .get {
                    stackTrace.toList()
                }
                .any {
                    get { methodName }.isEqualTo("makeHydrationFieldInstruction")
                }
        }

        return true
    }
}
