package graphql.nadel.engine.instrumentation

import graphql.execution.instrumentation.InstrumentationState
import graphql.nadel.engine.transform.hydration.NadelHydrationTransform
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationTransform
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.ChildStep
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.RootStep
import io.kotest.core.spec.style.DescribeSpec
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.jupiter.api.assertThrows

class NadelInstrumentationTimerTest : DescribeSpec({
    describe("time") {
        it("records the time on success") {
            // given
            var instrumentationParams: NadelInstrumentationTimingParameters? = null
            val instrumentation = object : NadelInstrumentation {
                override fun onStepTimed(parameters: NadelInstrumentationTimingParameters) {
                    assert(instrumentationParams == null) // Ensure only invoked once
                    instrumentationParams = parameters
                }
            }
            val timer = NadelInstrumentationTimer(
                instrumentation,
                userContext = null,
                instrumentationState = null,
            )

            // when
            timer.time(RootStep.ExecutionPlanning) {
                delay(128)
            }

            // then
            val params = requireNotNull(instrumentationParams)
            assert(params.step.name == "ExecutionPlanning")
            assert(params.duration.toMillis() in 128..256)
        }

        it("returns the result from the input function") {
            // given
            var instrumentationParams: NadelInstrumentationTimingParameters? = null
            val instrumentation = object : NadelInstrumentation {
                override fun onStepTimed(parameters: NadelInstrumentationTimingParameters) {
                    assert(instrumentationParams == null) // Ensure only invoked once
                }
            }
            val timer = NadelInstrumentationTimer(
                instrumentation,
                userContext = null,
                instrumentationState = null,
            )

            // when
            val result = timer.time(RootStep.ExecutionPlanning) {
                "Output from result"
            }

            // then
            assert(result == "Output from result")
        }

        it("emits time on exception") {
            // given
            var instrumentationParams: NadelInstrumentationTimingParameters? = null
            val instrumentation = object : NadelInstrumentation {
                override fun onStepTimed(parameters: NadelInstrumentationTimingParameters) {
                    assert(instrumentationParams == null) // Ensure only invoked once
                    instrumentationParams = parameters
                }
            }
            val timer = NadelInstrumentationTimer(
                instrumentation,
                userContext = null,
                instrumentationState = null,
            )

            // when
            val ex = assertThrows<UnsupportedOperationException> {
                timer.time(RootStep.QueryTransforming) {
                    delay(256)
                    throw UnsupportedOperationException("no-op")
                }
            }

            // then
            assert(ex.message == "no-op")

            val params = requireNotNull(instrumentationParams)
            assert(params.step.name == "QueryTransforming")
            assert(params.duration.toMillis() in 256..400)
            assert(params.exception === ex)
            assert(params.exception?.message == "no-op")
        }

        it("passes the user context and instrumentation state to the instrumentation") {
            // given
            var instrumentationParams: NadelInstrumentationTimingParameters? = null
            val instrumentation = object : NadelInstrumentation {
                override fun onStepTimed(parameters: NadelInstrumentationTimingParameters) {
                    assert(instrumentationParams == null) // Ensure only invoked once
                    instrumentationParams = parameters
                }
            }
            val userContext = mapOf("Test" to "Hello World")
            val state = object : InstrumentationState {
            }
            val timer = NadelInstrumentationTimer(
                instrumentation,
                userContext = userContext,
                instrumentationState = state,
            )

            // when
            val ex = assertThrows<UnsupportedOperationException> {
                timer.time(RootStep.QueryTransforming) {
                    delay(256)
                    throw UnsupportedOperationException("no-op")
                }
            }

            // then
            assert(ex.message == "no-op")

            val params = requireNotNull(instrumentationParams)

            val contextFromParams = params.getContext<Map<String, String>>()
            assert(contextFromParams === userContext)
            assert(contextFromParams?.get("Test") == "Hello World")

            val stateFromParams = params.getInstrumentationState<InstrumentationState>()
            assert(stateFromParams === state)
        }

        it("handles exceptions inside onStepTimed") {
            // given
            var instrumentationParams: NadelInstrumentationTimingParameters? = null
            val instrumentation = object : NadelInstrumentation {
                override fun onStepTimed(parameters: NadelInstrumentationTimingParameters) {
                    assert(instrumentationParams == null) // Ensure only invoked once
                    instrumentationParams = parameters
                    throw UnsupportedOperationException("No step timing")
                }
            }
            val timer = NadelInstrumentationTimer(
                instrumentation,
                userContext = null,
                instrumentationState = null,
            )

            // when
            val ex = assertThrows<UnsupportedOperationException> {
                timer.time(RootStep.QueryTransforming) {
                    delay(256)
                }
            }

            // then
            assert(ex.message == "No step timing")

            val params = requireNotNull(instrumentationParams)
            assert(params.duration.toMillis() in 256..400)
            assert(params.exception == null)
        }

        it("handles exceptions inside onStepTimed when already handling exception from function") {
            // given
            var instrumentationParams: NadelInstrumentationTimingParameters? = null
            val instrumentation = object : NadelInstrumentation {
                override fun onStepTimed(parameters: NadelInstrumentationTimingParameters) {
                    assert(instrumentationParams == null) // Ensure only invoked once
                    instrumentationParams = parameters
                    throw UnsupportedOperationException("No step timing")
                }
            }
            val timer = NadelInstrumentationTimer(
                instrumentation,
                userContext = null,
                instrumentationState = null,
            )

            // when
            val ex = assertThrows<UnsupportedOperationException> {
                timer.time(RootStep.QueryTransforming) {
                    delay(256)
                    throw IllegalArgumentException("ID is invalid")
                }
            }

            // then
            assert(ex.message == "No step timing")
            assert(ex.suppressedExceptions.single().message == "ID is invalid")

            val params = requireNotNull(instrumentationParams)
            assert(params.exception?.message == "ID is invalid")
        }
    }

    describe("BatchTimer") {
        it("time returns function result") {
            // given
            val instrumentationParams = mutableListOf<NadelInstrumentationTimingParameters>()
            val instrumentation = object : NadelInstrumentation {
                override fun onStepTimed(parameters: NadelInstrumentationTimingParameters) {
                    instrumentationParams.add(parameters)
                }
            }
            val timer = NadelInstrumentationTimer(
                instrumentation,
                userContext = null,
                instrumentationState = null,
            )

            // when
            val batchResult = timer.batch { batchTimer ->
                val timeResult = batchTimer.time(
                    ChildStep(parent = RootStep.ExecutionPlanning, NadelHydrationTransform::class),
                ) {
                    delay(64)
                    "Hello World"
                }

                assert(timeResult == "Hello World")

                "Bye"
            }

            // then
            assert(batchResult == "Bye")
        }

        it("exceptions inside function are thrown to caller") {
            // given
            val instrumentationParams = mutableListOf<NadelInstrumentationTimingParameters>()
            val instrumentation = object : NadelInstrumentation {
                override fun onStepTimed(parameters: NadelInstrumentationTimingParameters) {
                    instrumentationParams.add(parameters)
                }
            }
            val timer = NadelInstrumentationTimer(
                instrumentation,
                userContext = null,
                instrumentationState = null,
            )

            // when
            val ex = assertThrows<Throwable> {
                timer.batch { batchTimer ->
                    batchTimer.time(
                        ChildStep(parent = RootStep.ExecutionPlanning, NadelHydrationTransform::class),
                    ) {
                        delay(64)
                        throw UnsupportedOperationException("Bye")
                    }

                    @Suppress("UNREACHABLE_CODE") // Test is meant to fail if it reaches here
                    error("Code should not come reach here")
                }
            }

            // then
            assert(ex.message == "Bye")
            assert(instrumentationParams.isNotEmpty())

            val param = instrumentationParams.single()
            assert(param.duration.toMillis() in 64..100)
            assert(param.exception?.message == "Bye")
        }

        it("batch timer does not emit times until submit is invoked") {
            // given
            val instrumentationParams = mutableListOf<NadelInstrumentationTimingParameters>()
            val instrumentation = object : NadelInstrumentation {
                override fun onStepTimed(parameters: NadelInstrumentationTimingParameters) {
                    instrumentationParams.add(parameters)
                }
            }
            val timer = NadelInstrumentationTimer(
                instrumentation,
                userContext = null,
                instrumentationState = null,
            )
            val batchTimer = timer.batch()

            // when
            batchTimer.time(
                ChildStep(parent = RootStep.ExecutionPlanning, NadelHydrationTransform::class),
            ) {
                delay(64)
            }

            // then
            assert(instrumentationParams.isEmpty())

            // when
            batchTimer.submit()

            // then
            assert(instrumentationParams.isNotEmpty())
        }

        it("combines times together") {
            // given
            val instrumentationParams = mutableListOf<NadelInstrumentationTimingParameters>()
            val instrumentation = object : NadelInstrumentation {
                override fun onStepTimed(parameters: NadelInstrumentationTimingParameters) {
                    instrumentationParams.add(parameters)
                }
            }
            val timer = NadelInstrumentationTimer(
                instrumentation,
                userContext = null,
                instrumentationState = null,
            )

            // when
            timer.batch { batchTimer ->
                coroutineScope {
                    launch {
                        batchTimer.time(
                            ChildStep(parent = RootStep.ExecutionPlanning, NadelHydrationTransform::class),
                        ) { delay(64) }
                        batchTimer.time(
                            ChildStep(parent = RootStep.ExecutionPlanning, NadelHydrationTransform::class),
                        ) { delay(128) }
                        batchTimer.time(
                            ChildStep(parent = RootStep.ExecutionPlanning, NadelHydrationTransform::class),
                        ) { delay(128) }
                    }
                    launch {
                        batchTimer.time(
                            ChildStep(parent = RootStep.ExecutionPlanning, NadelBatchHydrationTransform::class),
                        ) { delay(32) }
                        batchTimer.time(
                            ChildStep(parent = RootStep.ExecutionPlanning, NadelBatchHydrationTransform::class),
                        ) { delay(32) }
                    }
                    launch {
                        batchTimer.time(
                            ChildStep(parent = RootStep.ResultTransforming, NadelBatchHydrationTransform::class),
                        ) { delay(256) }
                        batchTimer.time(
                            ChildStep(parent = RootStep.ResultTransforming, NadelBatchHydrationTransform::class),
                        ) { delay(64) }
                    }
                }
            }

            // then
            val planHydration = instrumentationParams.single {
                it.step.getFullName() == "ExecutionPlanning.NadelHydrationTransform"
            }
            assert(planHydration.exception == null)
            assert(planHydration.duration.toMillis() in 320..400)

            val planBatchHydration = instrumentationParams.single {
                it.step.getFullName() == "ExecutionPlanning.NadelBatchHydrationTransform"
            }
            assert(planBatchHydration.exception == null)
            assert(planBatchHydration.duration.toMillis() in 64..100)

            val resultTransformBatchHydration = instrumentationParams.single {
                it.step.getFullName() == "ResultTransforming.NadelBatchHydrationTransform"
            }
            assert(resultTransformBatchHydration.exception == null)
            assert(resultTransformBatchHydration.duration.toMillis() in 320..400)
        }
    }
})
