package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.nadel.Nadel
import graphql.nadel.NadelEngineContext
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformJavaCompat
import graphql.nadel.engine.transform.NadelTransformContext
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
import graphql.normalized.ExecutableNormalizedField
import kotlinx.coroutines.delay
import java.util.concurrent.CompletableFuture

private class MonitorEmitsTimingsTransform : NadelTransform<MonitorEmitsTimingsTransform.TransformContext> {
    object TransformContext : NadelTransformContext

    context(NadelEngineContext, NadelExecutionContext)
    override suspend fun isApplicable(
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): TransformContext? {
        delay(128)
        return TransformContext
    }

    context(NadelEngineContext, NadelExecutionContext, TransformContext)
    override suspend fun transformField(
        transformer: NadelQueryTransformer,
        field: ExecutableNormalizedField,
    ): NadelTransformFieldResult {
        delay(256)
        return NadelTransformFieldResult.unmodified(field)
    }

    context(NadelEngineContext, NadelExecutionContext, TransformContext)
    override suspend fun getResultInstructions(
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        nodes: JsonNodes,
    ): List<NadelResultInstruction> {
        delay(32)
        return listOf()
    }
}

private class JavaTimingTransform : NadelTransformJavaCompat<JavaTimingTransform.TransformContext> {
    object TransformContext : NadelTransformContext

    override fun isApplicable(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): CompletableFuture<TransformContext?> {
        return CompletableFuture.completedFuture(TransformContext)
    }

    override fun transformField(
        executionContext: NadelExecutionContext,
        transformer: NadelQueryTransformerJavaCompat,
        executionBlueprint: NadelOverallExecutionBlueprint,
        field: ExecutableNormalizedField,
        state: TransformContext,
    ): CompletableFuture<NadelTransformFieldResult> {
        return CompletableFuture.completedFuture(NadelTransformFieldResult.unmodified(field))
    }

    override fun getResultInstructions(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        state: TransformContext,
        nodes: JsonNodes,
    ): CompletableFuture<List<NadelResultInstruction>> {
        return CompletableFuture.completedFuture(emptyList())
    }
}

@UseHook
class `monitor-emits-timings` : EngineTestHook {
    private val stepsWitnessed = mutableSetOf<Step>()

    override val customTransforms: List<NadelTransform<out NadelTransformContext>>
        get() = listOf(
            MonitorEmitsTimingsTransform(),
            NadelTransformJavaCompat.create(JavaTimingTransform()),
        )

    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder.instrumentation(
            object : NadelInstrumentation {
                override fun onStepTimed(parameters: NadelInstrumentationTimingParameters) {
                    println(parameters)

                    stepsWitnessed.add(parameters.step)
                    val duration = parameters.duration

                    val step = parameters.step
                    if (step.name == MonitorEmitsTimingsTransform::class.simpleName) {
                        when (step.parent) {
                            ExecutionPlanning -> assert(duration.toMillis() in 128..256)
                            QueryTransforming -> assert(duration.toMillis() in 256..512)
                            ResultTransforming -> assert(duration.toMillis() in 32..128)
                            else -> {
                            }
                        }
                    }
                }
            },
        )
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

