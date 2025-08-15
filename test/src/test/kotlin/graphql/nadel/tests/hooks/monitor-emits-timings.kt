package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionInput
import graphql.nadel.engine.NadelOperationExecutionContext
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldContext
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformJavaCompat
import graphql.nadel.engine.transform.NadelTransformOperationContext
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.query.NadelQueryTransformerJavaCompat
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.instrumentation.parameters.NadelInstrumentationIsTimingEnabledParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.ChildStep
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.RootStep.ExecutionPlanning
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.RootStep.QueryTransforming
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.RootStep.ResultTransforming
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.Step
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.nadel.tests.util.NadelTransformAdapter
import graphql.nadel.time.NadelInternalLatencyTrackerImpl
import graphql.nadel.time.NadelStopwatch
import graphql.normalized.ExecutableNormalizedField
import java.time.Duration
import java.util.concurrent.CompletableFuture

private class MonitorEmitsTimingsTransform(
    private val delay: (Long) -> Unit,
) : NadelTransform<
    MonitorEmitsTimingsTransform.TransformOperationContext,
    MonitorEmitsTimingsTransform.TransformFieldContext,
    > {
    data class TransformOperationContext(
        override val parentContext: NadelOperationExecutionContext,
    ) : NadelTransformOperationContext()

    data class TransformFieldContext(
        override val parentContext: TransformOperationContext,
        override val overallField: ExecutableNormalizedField,
    ) : NadelTransformFieldContext<TransformOperationContext>()

    override suspend fun getTransformOperationContext(
        operationExecutionContext: NadelOperationExecutionContext,
    ): TransformOperationContext {
        return TransformOperationContext(operationExecutionContext)
    }

    override suspend fun getTransformFieldContext(
        transformContext: TransformOperationContext,
        overallField: ExecutableNormalizedField,
    ): TransformFieldContext? {
        delay(128)
        return TransformFieldContext(transformContext, overallField)
    }

    override suspend fun transformField(
        transformContext: TransformFieldContext,
        transformer: NadelQueryTransformer,
        field: ExecutableNormalizedField,
    ): NadelTransformFieldResult {
        delay(256)
        return NadelTransformFieldResult.unmodified(field)
    }

    override suspend fun transformResult(
        transformContext: TransformFieldContext,
        underlyingParentField: ExecutableNormalizedField?,
        resultNodes: JsonNodes,
    ): List<NadelResultInstruction> {
        delay(32)
        return listOf()
    }
}

private class JavaTimingTransform : NadelTransformJavaCompat<
    JavaTimingTransform.TransformOperationContext,
    JavaTimingTransform.TransformFieldContext,
    > {
    data class TransformOperationContext(
        override val parentContext: NadelOperationExecutionContext,
    ) : NadelTransformOperationContext()

    data class TransformFieldContext(
        override val parentContext: TransformOperationContext,
        override val overallField: ExecutableNormalizedField,
    ) : NadelTransformFieldContext<TransformOperationContext>()

    override fun getTransformOperationContext(
        operationExecutionContext: NadelOperationExecutionContext,
    ): CompletableFuture<TransformOperationContext> {
        return CompletableFuture.completedFuture(TransformOperationContext(operationExecutionContext))
    }

    override fun getTransformFieldContext(
        transformContext: TransformOperationContext,
        overallField: ExecutableNormalizedField,
    ): CompletableFuture<TransformFieldContext?> {
        return CompletableFuture.completedFuture(TransformFieldContext(transformContext, overallField))
    }

    override fun transformField(
        transformContext: TransformFieldContext,
        transformer: NadelQueryTransformerJavaCompat,
        field: ExecutableNormalizedField,
    ): CompletableFuture<NadelTransformFieldResult> {
        return CompletableFuture.completedFuture(NadelTransformFieldResult.unmodified(field))
    }

    override fun transformResult(
        transformContext: TransformFieldContext,
        underlyingParentField: ExecutableNormalizedField?,
        resultNodes: JsonNodes,
    ): CompletableFuture<List<NadelResultInstruction>> {
        return CompletableFuture.completedFuture(emptyList())
    }
}

@UseHook
class `monitor-emits-timings` : EngineTestHook {
    private var timeNs = 101812381L // Value doesn't matter, it's just the start value
    private val stopwatch = NadelStopwatch { timeNs }
    private val latencyTracker = NadelInternalLatencyTrackerImpl(stopwatch)

    private val stepsWitnessed = mutableSetOf<Step>()

    override val customTransforms: List<NadelTransform<*, *>>
        get() = listOf(
            MonitorEmitsTimingsTransform(::passTime),
            NadelTransformJavaCompat.create(JavaTimingTransform()),
        )

    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder.instrumentation(
            object : NadelInstrumentation {
                override fun isTimingEnabled(params: NadelInstrumentationIsTimingEnabledParameters): Boolean {
                    return true
                }

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

