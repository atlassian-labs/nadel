package graphql.nadel.instrumentation

import graphql.ExecutionResult
import graphql.execution.instrumentation.InstrumentationContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.language.Document
import graphql.nadel.engine.util.singleOfType
import graphql.nadel.instrumentation.ChainedNadelInstrumentation.ChainedInstrumentationState
import graphql.nadel.instrumentation.parameters.NadelInstrumentationCreateStateParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationExecuteOperationParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationOnExceptionParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationOnGraphQLErrorsParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationQueryExecutionParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationQueryValidationParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters
import graphql.nadel.test.mock
import graphql.nadel.test.spy
import graphql.validation.ValidationError
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.future.asDeferred
import java.util.concurrent.CompletableFuture

class ChainedNadelInstrumentationTest : DescribeSpec({
    describe("composite instrumentation state") {
        it("holds and returns individual states") {
            // given
            val chainedInstrumentation = ChainedNadelInstrumentation(
                // Creates 5 instrumentations to test
                (1..5)
                    .map { index ->
                        object : NadelInstrumentation {
                            override fun createState(parameters: NadelInstrumentationCreateStateParameters): InstrumentationState? {
                                return object : InstrumentationState {
                                    override fun toString(): String {
                                        return "state-$index"
                                    }
                                }
                            }

                            override fun hashCode(): Int {
                                return index
                            }
                        }
                    }
                    .shuffled()
            )

            // when
            val chainedState = chainedInstrumentation.createState(mock())

            // then
            assert(chainedState is ChainedInstrumentationState)
            chainedState as ChainedInstrumentationState

            chainedInstrumentation.getInstrumentations()
                .forEach { instrumentation ->
                    assert(chainedState.getState(instrumentation).toString() == "state-${instrumentation.hashCode()}")
                }
        }
    }

    describe("states") {
        class State : InstrumentationState {
            var something: Any? = null
        }

        class TestInstrumentation(val key: Any) : NadelInstrumentation {
            override fun createState(parameters: NadelInstrumentationCreateStateParameters): InstrumentationState? {
                return State().also { it.something = key }
            }

            override fun onStepTimed(parameters: NadelInstrumentationTimingParameters) {
                assert(parameters.getInstrumentationState<State>()?.something == key)
            }

            override fun beginQueryExecution(parameters: NadelInstrumentationQueryExecutionParameters): InstrumentationContext<ExecutionResult> {
                assert(parameters.getInstrumentationState<State>()?.something == key)
                return super.beginQueryExecution(parameters)
            }

            override fun beginParse(parameters: NadelInstrumentationQueryExecutionParameters): InstrumentationContext<Document> {
                assert(parameters.getInstrumentationState<State>()?.something == key)
                return super.beginParse(parameters)
            }

            override fun beginValidation(parameters: NadelInstrumentationQueryValidationParameters): InstrumentationContext<List<ValidationError>> {
                assert(parameters.getInstrumentationState<State>()?.something == key)
                return super.beginValidation(parameters)
            }

            override fun beginExecute(parameters: NadelInstrumentationExecuteOperationParameters): CompletableFuture<InstrumentationContext<ExecutionResult>> {
                assert(parameters.getInstrumentationState<State>()?.something == key)
                return super.beginExecute(parameters)
            }

            override fun instrumentExecutionResult(
                executionResult: ExecutionResult,
                parameters: NadelInstrumentationQueryExecutionParameters,
            ): CompletableFuture<ExecutionResult> {
                assert(parameters.getInstrumentationState<State>()?.something == key)
                return super.instrumentExecutionResult(executionResult, parameters)
            }
        }

        val chainedInstrumentation = ChainedNadelInstrumentation(
            // Creates 5 instrumentations to test
            (1..5).map { index ->
                spyk(TestInstrumentation(key = "test-$index"))
            },
        )

        val chainedState = chainedInstrumentation.createState(mock())

        it("passes on correct state for onStepTimed") {
            // when
            chainedInstrumentation.onStepTimed(mock { params ->
                every {
                    params.getInstrumentationState<InstrumentationState>()
                } returns chainedState

                every {
                    params.copy(any(), any(), any(), any(), any(), any())
                } answers { copyCall ->
                    val newState = copyCall.invocation.args.singleOfType<InstrumentationState>()
                    mock { newParams ->
                        every {
                            newParams.getInstrumentationState<InstrumentationState>()
                        } answers {
                            newState
                        }
                    }
                }
            })

            // then
            chainedInstrumentation.getInstrumentations()
                .map { it as TestInstrumentation }
                .forEach { instrumentation ->
                    val params = slot<NadelInstrumentationTimingParameters>()
                    verify(exactly = 1) {
                        instrumentation.onStepTimed(capture(params))
                    }

                    assert(params.isCaptured)
                    assert(params.captured.getInstrumentationState<State>()?.something == instrumentation.key)
                }
        }

        it("passes on correct state for beginQueryExecution") {
            chainedInstrumentation.beginQueryExecution(mock { params ->
                every {
                    params.getInstrumentationState<InstrumentationState>()
                } returns chainedState

                every {
                    params.copy(any(), any(), any(), any(), any(), any(), any())
                } answers { copyCall ->
                    val newState = copyCall.invocation.args.singleOfType<InstrumentationState>()
                    mock { newParams ->
                        every {
                            newParams.getInstrumentationState<InstrumentationState>()
                        } answers {
                            newState
                        }
                    }
                }
            })

            // then
            chainedInstrumentation.getInstrumentations()
                .map { it as TestInstrumentation }
                .forEach { instrumentation ->
                    val params = slot<NadelInstrumentationQueryExecutionParameters>()
                    verify(exactly = 1) {
                        instrumentation.beginQueryExecution(capture(params))
                    }

                    assert(params.isCaptured)
                    assert(params.captured.getInstrumentationState<State>()?.something == instrumentation.key)
                }
        }

        it("passes on correct state for beginParse") {
            chainedInstrumentation.beginParse(mock { params ->
                every {
                    params.getInstrumentationState<InstrumentationState>()
                } returns chainedState

                every {
                    params.copy(any(), any(), any(), any(), any(), any(), any())
                } answers { copyCall ->
                    val newState = copyCall.invocation.args.singleOfType<InstrumentationState>()
                    mock { newParams ->
                        every {
                            newParams.getInstrumentationState<InstrumentationState>()
                        } answers {
                            newState
                        }
                    }
                }
            })

            // then
            chainedInstrumentation.getInstrumentations()
                .map { it as TestInstrumentation }
                .forEach { instrumentation ->
                    val params = slot<NadelInstrumentationQueryExecutionParameters>()
                    verify(exactly = 1) {
                        instrumentation.beginParse(capture(params))
                    }

                    assert(params.isCaptured)
                    assert(params.captured.getInstrumentationState<State>()?.something == instrumentation.key)
                }
        }

        it("passes on correct state for beginValidation") {
            chainedInstrumentation.beginValidation(mock { params ->
                every {
                    params.getInstrumentationState<InstrumentationState>()
                } returns chainedState

                every {
                    params.copy(any(), any(), any(), any(), any())
                } answers { copyCall ->
                    val newState = copyCall.invocation.args.singleOfType<InstrumentationState>()
                    mock { newParams ->
                        every {
                            newParams.getInstrumentationState<InstrumentationState>()
                        } answers {
                            newState
                        }
                    }
                }
            })

            // then
            chainedInstrumentation.getInstrumentations()
                .map { it as TestInstrumentation }
                .forEach { instrumentation ->
                    val params = slot<NadelInstrumentationQueryValidationParameters>()
                    verify(exactly = 1) {
                        instrumentation.beginValidation(capture(params))
                    }

                    assert(params.isCaptured)
                    assert(params.captured.getInstrumentationState<State>()?.something == instrumentation.key)
                }
        }

        it("passes on correct state for beginExecute") {
            chainedInstrumentation.beginExecute(mock { params ->
                every {
                    params.getInstrumentationState<InstrumentationState>()
                } returns chainedState

                every {
                    params.copy(any(), any(), any(), any(), any(), any(), any())
                } answers { copyCall ->
                    val newState = copyCall.invocation.args.singleOfType<InstrumentationState>()
                    mock { newParams ->
                        every {
                            newParams.getInstrumentationState<InstrumentationState>()
                        } answers {
                            newState
                        }
                    }
                }
            })

            // then
            chainedInstrumentation.getInstrumentations()
                .map { it as TestInstrumentation }
                .forEach { instrumentation ->
                    val params = slot<NadelInstrumentationExecuteOperationParameters>()
                    verify(exactly = 1) {
                        instrumentation.beginExecute(capture(params))
                    }

                    assert(params.isCaptured)
                    assert(params.captured.getInstrumentationState<State>()?.something == instrumentation.key)
                }
        }

        it("passes on correct state for instrumentExecutionResult") {
            chainedInstrumentation.instrumentExecutionResult(mock(), mock { params ->
                every {
                    params.getInstrumentationState<InstrumentationState>()
                } returns chainedState

                every {
                    params.copy(any(), any(), any(), any(), any(), any(), any())
                } answers { copyCall ->
                    val newState = copyCall.invocation.args.singleOfType<InstrumentationState>()
                    mock { newParams ->
                        every {
                            newParams.getInstrumentationState<InstrumentationState>()
                        } answers {
                            newState
                        }
                    }
                }
            })

            // then
            chainedInstrumentation.getInstrumentations()
                .map { it as TestInstrumentation }
                .forEach { instrumentation ->
                    val params = slot<NadelInstrumentationQueryExecutionParameters>()
                    verify(exactly = 1) {
                        instrumentation.instrumentExecutionResult(any(), capture(params))
                    }

                    assert(params.isCaptured)
                    assert(params.captured.getInstrumentationState<State>()?.something == instrumentation.key)
                }
        }

        it("passes on correct state for onException") {
            // when
            chainedInstrumentation.onException(mock { params ->
                every {
                    params.getInstrumentationState<InstrumentationState>()
                } returns chainedState

                every {
                    params.copy(any(), any(), any())
                } answers { copyCall ->
                    val newState = copyCall.invocation.args.singleOfType<InstrumentationState>()
                    mock { newParams ->
                        every {
                            newParams.getInstrumentationState<InstrumentationState>()
                        } answers {
                            newState
                        }
                    }
                }
            })

            // then
            chainedInstrumentation.getInstrumentations()
                .map { it as TestInstrumentation }
                .forEach { instrumentation ->
                    val params = slot<NadelInstrumentationOnExceptionParameters>()
                    verify(exactly = 1) {
                        instrumentation.onException(capture(params))
                    }

                    assert(params.isCaptured)
                    assert(params.captured.getInstrumentationState<State>()?.something == instrumentation.key)
                }
        }

        it("passes on correct state for onGraphQLErrors") {
            // when
            chainedInstrumentation.onGraphQLErrors(mock { params ->
                every {
                    params.getInstrumentationState<InstrumentationState>()
                } returns chainedState

                every {
                    params.copy(any(), any(), any())
                } answers { copyCall ->
                    val newState = copyCall.invocation.args.singleOfType<InstrumentationState>()
                    mock { newParams ->
                        every {
                            newParams.getInstrumentationState<InstrumentationState>()
                        } answers {
                            newState
                        }
                    }
                }
            })

            // then
            chainedInstrumentation.getInstrumentations()
                .map { it as TestInstrumentation }
                .forEach { instrumentation ->
                    val params = slot<NadelInstrumentationOnGraphQLErrorsParameters>()
                    verify(exactly = 1) {
                        instrumentation.onGraphQLErrors(capture(params))
                    }

                    assert(params.isCaptured)
                    assert(params.captured.getInstrumentationState<State>()?.something == instrumentation.key)
                }
        }
    }

    describe("parameter delegation") {
        val chainedInstrumentation = ChainedNadelInstrumentation(
            (1..10).map {
                spy(
                    object : NadelInstrumentation {
                    },
                )
            }
        )

        val chainedState = chainedInstrumentation.createState(mock())

        it("passes on correct parameters for onStepTimed") {
            // given
            val paramsCopy = mock<NadelInstrumentationTimingParameters>()
            val params = mock<NadelInstrumentationTimingParameters> { params ->
                every { params.getInstrumentationState<InstrumentationState>() } returns chainedState
                every { params.copy(any(), any(), any(), any(), any(), any()) } returns paramsCopy
            }

            // when
            chainedInstrumentation.onStepTimed(params)

            // then
            chainedInstrumentation.getInstrumentations()
                .forEach { instrumentation ->
                    verify(exactly = 1) {
                        instrumentation.onStepTimed(paramsCopy)
                    }
                }
        }

        it("passes on correct parameters for beginQueryExecution") {
            // given
            val paramsCopy = mock<NadelInstrumentationQueryExecutionParameters>()
            val params = mock<NadelInstrumentationQueryExecutionParameters> { params ->
                every { params.getInstrumentationState<InstrumentationState>() } returns chainedState
                every { params.copy(any(), any(), any(), any(), any(), any(), any()) } returns paramsCopy
            }

            // when
            chainedInstrumentation.beginQueryExecution(params)

            // then
            chainedInstrumentation.getInstrumentations()
                .forEach { instrumentation ->
                    verify(exactly = 1) {
                        instrumentation.beginQueryExecution(paramsCopy)
                    }
                }
        }

        it("passes on correct parameters for beginParse") {
            // given
            val paramsCopy = mock<NadelInstrumentationQueryExecutionParameters>()
            val params = mock<NadelInstrumentationQueryExecutionParameters> { params ->
                every { params.getInstrumentationState<InstrumentationState>() } returns chainedState
                every { params.copy(any(), any(), any(), any(), any(), any(), any()) } returns paramsCopy
            }

            // when
            chainedInstrumentation.beginParse(params)

            // then
            chainedInstrumentation.getInstrumentations()
                .forEach { instrumentation ->
                    verify(exactly = 1) {
                        instrumentation.beginParse(paramsCopy)
                    }
                }
        }

        it("passes on correct parameters for beginValidation") {
            // given
            val paramsCopy = mock<NadelInstrumentationQueryValidationParameters>()
            val params = mock<NadelInstrumentationQueryValidationParameters> { params ->
                every { params.getInstrumentationState<InstrumentationState>() } returns chainedState
                every { params.copy(any(), any(), any(), any(), any()) } returns paramsCopy
            }

            // when
            chainedInstrumentation.beginValidation(params)

            // then
            chainedInstrumentation.getInstrumentations()
                .forEach { instrumentation ->
                    verify(exactly = 1) {
                        instrumentation.beginValidation(paramsCopy)
                    }
                }
        }

        it("passes on correct parameters for beginExecute") {
            // given
            val paramsCopy = mock<NadelInstrumentationExecuteOperationParameters>()
            val params = mock<NadelInstrumentationExecuteOperationParameters> { params ->
                every { params.getInstrumentationState<InstrumentationState>() } returns chainedState
                every { params.copy(any(), any(), any(), any(), any(), any(), any()) } returns paramsCopy
            }

            // when
            chainedInstrumentation.beginExecute(params).asDeferred().await()

            // then
            chainedInstrumentation.getInstrumentations()
                .forEach { instrumentation ->
                    verify(exactly = 1) {
                        instrumentation.beginExecute(paramsCopy)
                    }
                }
        }

        it("passes on correct parameters for instrumentExecutionResult") {
            // given
            val paramsCopy = mock<NadelInstrumentationQueryExecutionParameters>()
            val params = mock<NadelInstrumentationQueryExecutionParameters> { params ->
                every { params.getInstrumentationState<InstrumentationState>() } returns chainedState
                every { params.copy(any(), any(), any(), any(), any(), any(), any()) } returns paramsCopy
            }

            // when
            chainedInstrumentation.instrumentExecutionResult(mock(), params).asDeferred().await()

            // then
            chainedInstrumentation.getInstrumentations()
                .forEach { instrumentation ->
                    verify(exactly = 1) {
                        instrumentation.instrumentExecutionResult(any(), paramsCopy)
                    }
                }
        }

        it("passes on correct parameters for onException") {
            // given
            val paramsCopy = mock<NadelInstrumentationOnExceptionParameters>()
            val params = mock<NadelInstrumentationOnExceptionParameters> { params ->
                every { params.getInstrumentationState<InstrumentationState>() } returns chainedState
                every { params.copy(any(), any(), any()) } returns paramsCopy
            }

            // when
            chainedInstrumentation.onException(params)

            // then
            chainedInstrumentation.getInstrumentations()
                .forEach { instrumentation ->
                    verify(exactly = 1) {
                        instrumentation.onException(paramsCopy)
                    }
                }
        }

        it("passes on correct parameters for onGraphQLErrors") {
            // given
            val paramsCopy = mock<NadelInstrumentationOnGraphQLErrorsParameters>()
            val params = mock<NadelInstrumentationOnGraphQLErrorsParameters> { params ->
                every { params.getInstrumentationState<InstrumentationState>() } returns chainedState
                every { params.copy(any(), any(), any()) } returns paramsCopy
            }

            // when
            chainedInstrumentation.onGraphQLErrors(params)

            // then
            chainedInstrumentation.getInstrumentations()
                .forEach { instrumentation ->
                    verify(exactly = 1) {
                        instrumentation.onGraphQLErrors(paramsCopy)
                    }
                }
        }
    }
})
