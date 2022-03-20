package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.execution.instrumentation.InstrumentationContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.language.Document
import graphql.nadel.Nadel
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.instrumentation.parameters.NadelInstrumentationCreateStateParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationExecuteOperationParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationQueryExecutionParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationQueryValidationParameters
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.validation.ValidationError
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.util.concurrent.CompletableFuture

@UseHook
class `instrumentation-is-called` : EngineTestHook {
    var instrumentationCalled = 0
    var instrumentationParseCalled = 0
    var instrumentationValidateCalled = 0
    var instrumentationExecuteCalled = 0

    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .instrumentation(object : NadelInstrumentation {
                override fun createState(parameters: NadelInstrumentationCreateStateParameters): InstrumentationState {
                    instrumentationCalled++
                    return object : InstrumentationState {
                    }
                }

                override fun beginParse(parameters: NadelInstrumentationQueryExecutionParameters): InstrumentationContext<Document> {
                    instrumentationParseCalled++
                    return super.beginParse(parameters)
                }

                override fun beginValidation(parameters: NadelInstrumentationQueryValidationParameters): InstrumentationContext<List<ValidationError>> {
                    instrumentationValidateCalled++
                    return super.beginValidation(parameters)
                }

                override fun beginExecute(parameters: NadelInstrumentationExecuteOperationParameters): CompletableFuture<InstrumentationContext<ExecutionResult>> {
                    instrumentationExecuteCalled++
                    return super.beginExecute(parameters)
                }
            })
    }

    override fun assertResult(result: ExecutionResult) {
        expectThat(instrumentationCalled).isEqualTo(1)
        expectThat(instrumentationParseCalled).isEqualTo(1)
        expectThat(instrumentationValidateCalled).isEqualTo(1)
        expectThat(instrumentationExecuteCalled).isEqualTo(1)
    }
}
