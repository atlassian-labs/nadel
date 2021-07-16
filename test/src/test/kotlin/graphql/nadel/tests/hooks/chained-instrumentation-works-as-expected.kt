package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.execution.instrumentation.InstrumentationContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.language.AstPrinter
import graphql.language.Document
import graphql.nadel.Nadel
import graphql.nadel.instrumentation.ChainedNadelInstrumentation
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.instrumentation.parameters.NadelInstrumentationCreateStateParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationExecuteOperationParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationQueryExecutionParameters
import graphql.nadel.instrumentation.parameters.NadelNadelInstrumentationQueryValidationParameters
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.KeepHook
import graphql.nadel.tests.NadelEngineType
import graphql.parser.Parser
import graphql.validation.ValidationError
import strikt.api.expectThat
import strikt.assertions.hasEntry
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import java.util.concurrent.CompletableFuture

@KeepHook
class `chained-instrumentation-works-as-expected` : EngineTestHook {
    class TestState : InstrumentationState

    var instrumentationCalled = 0
    var instrumentationParseCalled = 0
    var instrumentationValidateCalled = 0
    var instrumentationExecuteCalled = 0

    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        val instrumentation = object : NadelInstrumentation {
            override fun createState(parameters: NadelInstrumentationCreateStateParameters): InstrumentationState {
                return TestState()
            }

            override fun beginQueryExecution(parameters: NadelInstrumentationQueryExecutionParameters): InstrumentationContext<ExecutionResult> {
                instrumentationCalled++

                expectThat(parameters)
                    .get {
                        getInstrumentationState<InstrumentationState>()
                    }
                    .isA<TestState>()

                expectThat(parameters)
                    .get {
                        val document = Parser.parse(query)
                        AstPrinter.printAstCompact(document)
                    }
                    .isEqualTo("query OpName {hello {name} hello {id}}")

                expectThat(parameters).get { variables }
                    .hasSize(1)
                    .hasEntry("var1", "val1")

                expectThat(parameters).get { operation }
                    .isNull()

                return super.beginQueryExecution(parameters)
            }

            override fun beginParse(parameters: NadelInstrumentationQueryExecutionParameters): InstrumentationContext<Document> {
                instrumentationParseCalled++
                return super.beginParse(parameters)
            }

            override fun beginValidation(parameters: NadelNadelInstrumentationQueryValidationParameters): InstrumentationContext<MutableList<ValidationError>> {
                instrumentationValidateCalled++
                return super.beginValidation(parameters)
            }

            override fun beginExecute(parameters: NadelInstrumentationExecuteOperationParameters): CompletableFuture<InstrumentationContext<ExecutionResult>> {
                instrumentationExecuteCalled++
                return super.beginExecute(parameters)
            }
        }
        return builder
            .instrumentation(
                ChainedNadelInstrumentation(listOf(instrumentation, instrumentation)),
            )
    }

    override fun assertResult(engineType: NadelEngineType, result: ExecutionResult) {
        expectThat(instrumentationCalled).isEqualTo(2)
        expectThat(instrumentationParseCalled).isEqualTo(2)
        expectThat(instrumentationValidateCalled).isEqualTo(2)
        expectThat(instrumentationExecuteCalled).isEqualTo(2)
    }
}
