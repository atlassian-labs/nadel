package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.execution.instrumentation.InstrumentationContext
import graphql.nadel.Nadel
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.instrumentation.parameters.NadelInstrumentationExecuteOperationParameters
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.KeepHook
import graphql.nadel.tests.NadelEngineType
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import java.util.concurrent.CompletableFuture

@KeepHook
class `can-execute-document-with-multiple-operation-definitions` : EngineTestHook {
    var hasCalledBeginExecute = false

    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder
            .instrumentation(object : NadelInstrumentation {
                override fun beginExecute(
                    parameters: NadelInstrumentationExecuteOperationParameters,
                ): CompletableFuture<InstrumentationContext<ExecutionResult>> {
                    hasCalledBeginExecute = true

                    expectThat(parameters.operationDefinition)
                        .get { name }
                        .isEqualTo("Test")

                    return super.beginExecute(parameters)
                }
            })
    }

    override fun assertResult(engineType: NadelEngineType, result: ExecutionResult) {
        expectThat(hasCalledBeginExecute).isTrue()
    }
}
