package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.nadel.Nadel
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.ChildStep
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.RootStep.ExecutionPlanning
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.RootStep.QueryTransforming
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters.RootStep.ResultTransforming
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.normalized.ExecutableNormalizedField
import kotlinx.coroutines.delay

private class MonitorEmitsTimingsTransform : NadelTransform<Unit> {
    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
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

@UseHook
class `monitor-emits-timings` : EngineTestHook {
    private val stepsWitnessed = mutableSetOf<NadelInstrumentationTimingParameters.Step>()

    override val customTransforms: List<NadelTransform<out Any>>
        get() = listOf(
            MonitorEmitsTimingsTransform(),
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
                // steps + steps.map { step ->
                //     ChildStep(parent = step, transform = MonitorEmitsTimingsTransform::class)
                // }
                // todo: uncomment code above once sub timings are available on all steps
                steps + ChildStep(parent = ExecutionPlanning, transform = MonitorEmitsTimingsTransform::class)
            }

        assert(stepsWitnessed.containsAll(expected))
    }
}
