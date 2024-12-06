package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionInput
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformJavaCompat
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.query.NadelQueryTransformerJavaCompat
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.ChildStep
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.RootStep.ExecutionPlanning
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.RootStep.QueryTransforming
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.RootStep.ResultTransforming
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.Step
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.nadel.tests.util.NadelTransformAdapter
import graphql.nadel.time.NadelInternalLatencyTracker
import graphql.nadel.time.NadelStopwatch
import graphql.normalized.ExecutableNormalizedField
import java.time.Duration
import java.util.concurrent.CompletableFuture

private class MonitorEmitsTimingsTransform(
    private val delay: (Long) -> Unit,
) : NadelTransform<Unit> {
    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): Unit? {
        delay(128)
        return Unit
    }

    override suspend fun transformField(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        transformer: NadelQueryTransformer,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
        state: Unit,
    ): NadelTransformFieldResult {
        delay(256)
        return NadelTransformFieldResult.unmodified(field)
    }

    override suspend fun getResultInstructions(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        state: Unit,
        nodes: JsonNodes,
    ): List<NadelResultInstruction> {
        delay(32)
        return listOf()
    }
}

private class JavaTimingTransform : NadelTransformJavaCompat<Unit> {
    override fun isApplicable(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): CompletableFuture<Unit?> {
        return CompletableFuture.completedFuture(Unit)
    }

    override fun transformField(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        transformer: NadelQueryTransformerJavaCompat,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
        state: Unit,
    ): CompletableFuture<NadelTransformFieldResult> {
        return CompletableFuture.completedFuture(NadelTransformFieldResult.unmodified(field))
    }

    override fun getResultInstructions(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        state: Unit,
        nodes: JsonNodes,
    ): CompletableFuture<List<NadelResultInstruction>> {
        return CompletableFuture.completedFuture(emptyList())
    }
}

@UseHook
class `monitor-emits-timings` : EngineTestHook {
    private var timeNs = 101812381L // Value doesn't matter, it's just the start value
    private val stopwatch = NadelStopwatch { timeNs }
    private val latencyTracker = NadelInternalLatencyTracker.Default(stopwatch)

    private val stepsWitnessed = mutableSetOf<Step>()

    override val customTransforms: List<NadelTransform<out Any>>
        get() = listOf(
            MonitorEmitsTimingsTransform(::passTime),
            NadelTransformJavaCompat.create(JavaTimingTransform()),
        )

    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder.instrumentation(
            object : NadelInstrumentation {
                override fun onStepTimed(parameters: NadelInstrumentationTimingParameters) {
                    println(parameters)

                    stepsWitnessed.add(parameters.step)
                    val duration = parameters.internalLatency

                    val step = parameters.step
                    if (step.name == MonitorEmitsTimingsTransform::class.simpleName) {
                        when (step.parent) {
                            ExecutionPlanning -> assert(duration.toMillis() == 128L)
                            QueryTransforming -> assert(duration.toMillis() == 256L)
                            ResultTransforming -> assert(duration.toMillis() == 32L)
                            else -> {
                            }
                        }
                    }
                }
            },
        )
    }

    override fun makeExecutionInput(builder: NadelExecutionInput.Builder): NadelExecutionInput.Builder {
        stopwatch.start()
        return super.makeExecutionInput(builder)
            .latencyTracker(latencyTracker)
    }

    override fun assertResult(result: ExecutionResult) {
        val expected = setOf(ExecutionPlanning, QueryTransforming, ResultTransforming)
            .let { steps ->
                // Add child steps for this transform
                steps + steps
                    // todo: remove filter transform once sub timings are available on all steps
                    .filter {
                        it == ExecutionPlanning
                    }
                    .flatMap { step ->
                        listOf(
                            ChildStep.create(parent = step, name = "MonitorEmitsTimingsTransform"),
                            ChildStep.create(parent = step, name = "JavaTimingTransform"),
                        )
                    }
            }

        assert(stepsWitnessed.containsAll(expected))
    }

    private fun passTime(ms: Long) {
        timeNs += Duration.ofMillis(ms).toNanos()
    }
}

/**
 * This is an issue with our separated test modules. I think we might want to merge those back into one.
 */
fun ChildStep.Companion.create(parent: Step, name: String): ChildStep {
    val n = name
    return ChildStep(
        parent,
        object : NadelTransformAdapter {
            override val name: String
                get() = n
        },
    )
}

