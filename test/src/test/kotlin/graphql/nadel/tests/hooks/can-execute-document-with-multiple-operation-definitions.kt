package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.execution.instrumentation.InstrumentationContext
import graphql.nadel.Nadel
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.instrumentation.parameters.NadelInstrumentationExecuteOperationParameters
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import java.util.concurrent.CompletableFuture

@UseHook
class `can-execute-document-with-multiple-operation-definitions` : EngineTestHook {
    var hasCalledBeginExecute = false

    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
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

    override fun assertResult(result: ExecutionResult) {
        expectThat(hasCalledBeginExecute).isTrue()
    }
}
