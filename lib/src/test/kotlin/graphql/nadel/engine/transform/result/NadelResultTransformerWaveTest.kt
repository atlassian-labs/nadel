package graphql.nadel.engine.transform.result

import graphql.GraphqlErrorBuilder
import graphql.nadel.NadelServiceExecutionResultImpl
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.instrumentation.NadelInstrumentationTimer
import graphql.nadel.engine.plan.NadelExecutionPlan
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformServiceExecutionContext
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters
import graphql.nadel.test.mock
import graphql.normalized.ExecutableNormalizedField
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

class NadelResultTransformerWaveTest {
    @Test
    fun `schedules native and legacy transforms while preserving global invocation order`() = runTest {
        val waveTransform = RecordingWaveTransform()
        val legacyTransform = RecordingLegacyTransform()
        val service = mock<Service>()
        val firstField = mock<ExecutableNormalizedField>()
        val secondField = mock<ExecutableNormalizedField>()
        val firstUnderlyingField = mock<ExecutableNormalizedField>(relaxed = true)
        val secondUnderlyingField = mock<ExecutableNormalizedField>(relaxed = true)
        val executionPlan = NadelExecutionPlan(
            transformationSteps = linkedMapOf(
                firstField to listOf(
                    step(service, firstField, waveTransform, "wave-first"),
                    step(service, firstField, legacyTransform, "legacy-first"),
                ),
                secondField to listOf(
                    step(service, secondField, waveTransform, "wave-second"),
                    step(service, secondField, legacyTransform, "legacy-second"),
                ),
            ),
            transformContexts = linkedMapOf(
                waveTransform to null,
                legacyTransform to null,
            ),
        )
        val result = NadelServiceExecutionResultImpl()

        NadelResultTransformer(mock(relaxed = true)).transform(
            executionContext = executionContext(),
            serviceExecutionContext = mock(relaxed = true),
            executionPlan = executionPlan,
            artificialFields = emptyList(),
            overallToUnderlyingFields = mapOf(
                firstField to listOf(firstUnderlyingField),
                secondField to listOf(secondUnderlyingField),
            ),
            service = service,
            result = result,
        )

        assertEquals(1, waveTransform.waveCalls.get())
        assertEquals(
            listOf(
                NadelResultTransformInvocationId(0),
                NadelResultTransformInvocationId(2),
            ),
            waveTransform.invocationIds,
        )
        assertEquals(
            listOf("wave-first", "wave-second"),
            waveTransform.invocationStates,
        )
        assertEquals(
            listOf(firstField, secondField),
            waveTransform.overallFields,
        )

        assertEquals(2, legacyTransform.invocationCalls.size)
        assertEquals(
            setOf("legacy-first", "legacy-second"),
            legacyTransform.invocationCalls.toSet(),
        )

        assertEquals(
            listOf(
                "wave:wave-first",
                "legacy:legacy-first",
                "wave:wave-second",
                "legacy:legacy-second",
            ),
            result.errors.map { error -> error?.get("message") },
        )
        assertEquals(1, waveTransform.onCompleteCalls.get())
        assertEquals(1, legacyTransform.onCompleteCalls.get())
    }

    private fun executionContext(): NadelExecutionContext {
        val timer = NadelInstrumentationTimer(
            isEnabled = false,
            ticker = { Duration.ZERO },
            instrumentation = object : NadelInstrumentation {},
            userContext = null,
            instrumentationState = null,
        )
        return mock(relaxed = true) { executionContext ->
            every { executionContext.timer } returns timer
            every { executionContext.hydrationDetails } returns null
        }
    }

    private fun step(
        service: Service,
        field: ExecutableNormalizedField,
        transform: NadelTransform<Any>,
        state: Any,
    ): NadelExecutionPlan.Step<Any> {
        val timingStep = NadelInstrumentationTimingParameters.ChildStep(
            parent = NadelInstrumentationTimingParameters.RootStep.ResultTransforming,
            transform = transform,
        )
        return NadelExecutionPlan.Step(
            service = service,
            field = field,
            transform = transform,
            queryTransformTimingStep = timingStep,
            resultTransformTimingStep = timingStep,
            state = state,
        )
    }

    private abstract class TestTransform : NadelTransform<Any> {
        override suspend fun isApplicable(
            executionContext: NadelExecutionContext,
            serviceExecutionContext: NadelServiceExecutionContext,
            executionBlueprint: NadelOverallExecutionBlueprint,
            services: Map<String, Service>,
            service: Service,
            overallField: ExecutableNormalizedField,
            transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
            hydrationDetails: ServiceExecutionHydrationDetails?,
        ): Any? {
            return null
        }

        override suspend fun transformField(
            executionContext: NadelExecutionContext,
            serviceExecutionContext: NadelServiceExecutionContext,
            transformer: NadelQueryTransformer,
            executionBlueprint: NadelOverallExecutionBlueprint,
            service: Service,
            field: ExecutableNormalizedField,
            state: Any,
            transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
        ): NadelTransformFieldResult {
            return NadelTransformFieldResult.unmodified(field)
        }

        protected fun errorInstruction(message: String): NadelResultInstruction {
            return NadelResultInstruction.AddError(
                GraphqlErrorBuilder.newError()
                    .message(message)
                    .build(),
            )
        }
    }

    private class RecordingWaveTransform :
        TestTransform(),
        NadelResultWaveTransform<Any> {
        val waveCalls = AtomicInteger()
        val onCompleteCalls = AtomicInteger()
        var invocationIds = emptyList<NadelResultTransformInvocationId>()
        var invocationStates = emptyList<Any>()
        var overallFields = emptyList<ExecutableNormalizedField>()

        override suspend fun getResultInstructions(
            wave: NadelResultTransformWave<Any>,
        ): NadelResultTransformOutput {
            waveCalls.incrementAndGet()
            invocationIds = wave.invocations.map { invocation -> invocation.id }
            invocationStates = wave.invocations.map { invocation -> invocation.state }
            overallFields = wave.invocations.map { invocation -> invocation.overallField }

            val deliberatelyReversedOutput = wave.invocations
                .asReversed()
                .associateTo(linkedMapOf()) { invocation ->
                    invocation.id to listOf(
                        errorInstruction("wave:${invocation.state}"),
                    )
                }
            return NadelResultTransformOutput.forWave(
                wave = wave,
                instructionsByInvocationId = deliberatelyReversedOutput,
            )
        }

        override suspend fun getResultInstructions(
            executionContext: NadelExecutionContext,
            serviceExecutionContext: NadelServiceExecutionContext,
            executionBlueprint: NadelOverallExecutionBlueprint,
            service: Service,
            overallField: ExecutableNormalizedField,
            underlyingParentField: ExecutableNormalizedField?,
            result: ServiceExecutionResult,
            state: Any,
            nodes: JsonNodes,
            transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
        ): List<NadelResultInstruction> {
            error("Native wave transforms must not use the legacy result callback")
        }

        override suspend fun onComplete(
            executionContext: NadelExecutionContext,
            serviceExecutionContext: NadelServiceExecutionContext,
            executionBlueprint: NadelOverallExecutionBlueprint,
            service: Service,
            result: ServiceExecutionResult,
            nodes: JsonNodes,
            transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
        ) {
            onCompleteCalls.incrementAndGet()
        }
    }

    private class RecordingLegacyTransform : TestTransform() {
        val invocationCalls: MutableList<Any> =
            Collections.synchronizedList(mutableListOf())
        val onCompleteCalls = AtomicInteger()

        override suspend fun getResultInstructions(
            executionContext: NadelExecutionContext,
            serviceExecutionContext: NadelServiceExecutionContext,
            executionBlueprint: NadelOverallExecutionBlueprint,
            service: Service,
            overallField: ExecutableNormalizedField,
            underlyingParentField: ExecutableNormalizedField?,
            result: ServiceExecutionResult,
            state: Any,
            nodes: JsonNodes,
            transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
        ): List<NadelResultInstruction> {
            invocationCalls.add(state)
            return listOf(errorInstruction("legacy:$state"))
        }

        override suspend fun onComplete(
            executionContext: NadelExecutionContext,
            serviceExecutionContext: NadelServiceExecutionContext,
            executionBlueprint: NadelOverallExecutionBlueprint,
            service: Service,
            result: ServiceExecutionResult,
            nodes: JsonNodes,
            transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
        ) {
            onCompleteCalls.incrementAndGet()
        }
    }
}
