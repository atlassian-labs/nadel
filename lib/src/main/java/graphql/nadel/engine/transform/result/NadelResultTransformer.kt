package graphql.nadel.engine.transform.result

import graphql.GraphQLError
import graphql.incremental.DeferPayload
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.instrumentation.NadelInstrumentationTimer
import graphql.nadel.engine.plan.AnyNadelExecutionPlanStep
import graphql.nadel.engine.plan.NadelExecutionPlan
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.transform.result.json.NadelResultView
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.MutableJsonMap
import graphql.nadel.engine.util.queryPath
import graphql.nadel.engine.util.toGraphQLError
import graphql.normalized.ExecutableNormalizedField
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal class NadelResultTransformer(private val executionBlueprint: NadelOverallExecutionBlueprint) {
    suspend fun transform(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionPlan: NadelExecutionPlan,
        artificialFields: List<ExecutableNormalizedField>,
        overallToUnderlyingFields: Map<ExecutableNormalizedField, List<ExecutableNormalizedField>>,
        service: Service,
        result: ServiceExecutionResult,
    ): ServiceExecutionResult {
        val resultView = makeResultView(
            executionContext = executionContext,
            data = result.data,
        )
        val mutations = getMutationInstructions(
            executionContext,
            serviceExecutionContext,
            executionPlan,
            artificialFields,
            overallToUnderlyingFields,
            service,
            result,
            resultView,
        )
        mutate(
            result = result,
            mutations = mutations,
            resultView = resultView,
        )
        return result
    }

    suspend fun transform(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionPlan: NadelExecutionPlan,
        artificialFields: List<ExecutableNormalizedField>,
        overallToUnderlyingFields: Map<ExecutableNormalizedField, List<ExecutableNormalizedField>>,
        service: Service,
        result: ServiceExecutionResult,
        deferPayload: DeferPayload,
    ): DeferPayload {
        val resultView = makeResultView(
            executionContext = executionContext,
            data = deferPayload.getData<JsonMap?>() ?: emptyMap(),
            responsePath = deferPayload.path,
        )
        val mutations = getMutationInstructions(
            executionContext,
            serviceExecutionContext,
            executionPlan,
            artificialFields,
            overallToUnderlyingFields,
            service,
            result,
            resultView,
        )
        mutate(
            result = deferPayload,
            mutations = mutations,
            resultView = resultView,
        )
        return deferPayload
    }

    private suspend fun getMutationInstructions(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionPlan: NadelExecutionPlan,
        artificialFields: List<ExecutableNormalizedField>,
        overallToUnderlyingFields: Map<ExecutableNormalizedField, List<ExecutableNormalizedField>>,
        service: Service,
        result: ServiceExecutionResult,
        resultView: NadelResultView,
    ): List<NadelResultMutation> {
        val nodes = resultView.nodes
        val contextByTransform = executionPlan.transformContexts
        val invocations = getResultTransformInvocations(
            executionPlan = executionPlan,
            overallToUnderlyingFields = overallToUnderlyingFields,
        )
        val asyncTransformOutputs =
            ArrayList<Deferred<NadelResultTransformOutput>>()
        lateinit var removeArtificialFieldInstructions: Deferred<List<NadelResultInstruction>>

        coroutineScope {
            executionContext.timer.batch { timer ->
                invocations
                    .groupBy { invocation ->
                        invocation.step.transform
                    }
                    .forEach { (transform, transformInvocations) ->
                        val wave = NadelResultTransformWave(
                            context = NadelResultTransformContext(
                                executionContext = executionContext,
                                serviceExecutionContext = serviceExecutionContext,
                                executionBlueprint = executionBlueprint,
                                service = service,
                                result = result,
                                resultView = resultView,
                                transformServiceExecutionContext = contextByTransform[transform],
                            ),
                            invocations = transformInvocations.map { invocation ->
                                invocation.transformInvocation
                            },
                        )

                        asyncTransformOutputs.add(
                            async {
                                executeResultTransformWave(
                                    transform = transform,
                                    scheduledInvocations = transformInvocations,
                                    wave = wave,
                                    timer = timer,
                                )
                            },
                        )
                    }
            }

            removeArtificialFieldInstructions =
                async {
                    getRemoveArtificialFieldInstructions(artificialFields, nodes)
                }
        }

        val mutationsByInvocationId =
            asyncTransformOutputs
                .awaitAll()
                .flatMap { output ->
                    output.mutationsByInvocationId.entries
                }
                .associate { (invocationId, mutations) ->
                    invocationId to mutations
                }

        val mutations = buildList {
            invocations.forEach { invocation ->
                addAll(mutationsByInvocationId[invocation.transformInvocation.id].orEmpty())
            }
            addAll(
                removeArtificialFieldInstructions
                    .await()
                    .map(NadelResultMutation::Instruction),
            )
        }

        coroutineScope {
            contextByTransform.forEach { (transform, transformServiceExecutionContext) ->
                launch {
                    transform.onComplete(
                        executionContext,
                        serviceExecutionContext,
                        executionBlueprint,
                        service,
                        result,
                        nodes,
                        transformServiceExecutionContext
                    )
                }
            }
        }
        return mutations
    }

    /**
     * Executes one semantic result wave.
     *
     * Native wave transforms process it as one timed unit. Public invocation-based transforms
     * are adapted into independently timed tasks, preserving their existing execution behavior.
     */
    private suspend fun executeResultTransformWave(
        transform: NadelTransform<Any>,
        scheduledInvocations: List<ScheduledResultTransformInvocation>,
        wave: NadelResultTransformWave<Any>,
        timer: NadelInstrumentationTimer.BatchTimer,
    ): NadelResultTransformOutput {
        @Suppress("UNCHECKED_CAST")
        val waveTransform = transform as? NadelResultWaveTransform<Any>
        return if (waveTransform != null) {
            timer.time(scheduledInvocations.first().step.resultTransformTimingStep) {
                waveTransform.getResultInstructions(wave)
            }
        } else {
            val context = wave.context
            coroutineScope {
                val instructionsByInvocationId = scheduledInvocations
                    .map { scheduledInvocation ->
                        val invocation = scheduledInvocation.transformInvocation
                        invocation.id to async {
                            timer.time(
                                scheduledInvocation.step.resultTransformTimingStep,
                            ) {
                                transform.getResultInstructions(
                                    context.executionContext,
                                    context.serviceExecutionContext,
                                    context.executionBlueprint,
                                    context.service,
                                    invocation.overallField,
                                    invocation.underlyingParentField,
                                    context.result,
                                    invocation.state,
                                    context.nodes,
                                    context.transformServiceExecutionContext,
                                )
                            }
                        }
                    }
                    .associate { (invocationId, instructions) ->
                        invocationId to instructions.await()
                    }

                NadelResultTransformOutput.forWave(
                    wave = wave,
                    instructionsByInvocationId = instructionsByInvocationId,
                )
            }
        }
    }

    private fun getResultTransformInvocations(
        executionPlan: NadelExecutionPlan,
        overallToUnderlyingFields: Map<ExecutableNormalizedField, List<ExecutableNormalizedField>>,
    ): List<ScheduledResultTransformInvocation> {
        val invocations = mutableListOf<ScheduledResultTransformInvocation>()

        for ((field, steps) in executionPlan.transformationSteps) {
            val underlyingFields = overallToUnderlyingFields[field]
            if (underlyingFields.isNullOrEmpty()) continue

            for (step in steps) {
                invocations.add(
                    ScheduledResultTransformInvocation(
                        step = step,
                        transformInvocation = NadelResultTransformInvocation(
                            id = NadelResultTransformInvocationId(
                                ordinal = invocations.size,
                            ),
                            overallField = field,
                            underlyingParentField = underlyingFields.first().parent,
                            state = step.state,
                        ),
                    ),
                )
            }
        }

        return invocations
    }

    private data class ScheduledResultTransformInvocation(
        val step: AnyNadelExecutionPlanStep,
        val transformInvocation: NadelResultTransformInvocation<Any>,
    )

    private fun makeResultView(
        executionContext: NadelExecutionContext,
        data: JsonMap,
        responsePath: List<Any>? = null,
    ): NadelResultView {
        return when {
            executionContext.hydrationDetails != null -> NadelResultView.unaddressable(
                data = data,
                queryPrefix = responsePath
                    ?.let(NadelQueryPath::fromResultPath)
                    ?: NadelQueryPath.root,
            )
            responsePath != null -> NadelResultView.deferred(
                data = data,
                path = responsePath,
            )
            else -> NadelResultView.root(data)
        }
    }

    private fun mutate(
        result: ServiceExecutionResult,
        mutations: List<NadelResultMutation>,
        resultView: NadelResultView,
    ) {
        mutate(mutations, resultView) { error ->
            process(
                error = error,
                errors = result.errors,
            )
        }
    }

    private fun mutate(
        result: DeferPayload,
        mutations: List<NadelResultMutation>,
        resultView: NadelResultView,
    ) {
        mutate(mutations, resultView) { error ->
            processGraphQLError(
                error = error,
                errors = result.errors,
            )
        }
    }

    private inline fun mutate(
        mutations: List<NadelResultMutation>,
        resultView: NadelResultView,
        addError: (GraphQLError) -> Unit,
    ) {
        applyDataMutations(mutations).forEach { errorMutation ->
            val error = when (errorMutation) {
                is NadelResultMutation.Instruction -> {
                    val instruction = errorMutation.instruction
                    check(instruction is NadelResultInstruction.AddError) {
                        "Expected an error instruction"
                    }
                    instruction.error
                }
                is NadelResultMutation.AddErrorAt -> getLocatedError(
                    errorMutation,
                    resultView,
                )
            }
            addError(error)
        }
    }

    /**
     * Structural mutations run before errors are located so error paths observe the final
     * client-shaped result rather than underlying-service aliases.
     */
    private fun applyDataMutations(
        mutations: List<NadelResultMutation>,
    ): List<NadelResultMutation> {
        return buildList {
            mutations.forEach { mutation ->
                when (mutation) {
                    is NadelResultMutation.AddErrorAt -> add(mutation)
                    is NadelResultMutation.Instruction -> when (val instruction = mutation.instruction) {
                        is NadelResultInstruction.Set -> process(instruction)
                        is NadelResultInstruction.Remove -> process(instruction)
                        is NadelResultInstruction.AddError -> add(mutation)
                    }
                }
            }
        }
    }

    private fun process(
        instruction: NadelResultInstruction.Set,
    ) {
        @Suppress("UNCHECKED_CAST")
        val map = instruction.subject.value as? MutableJsonMap ?: return
        map[instruction.key.value] = instruction.newValue?.value
    }

    private fun process(
        instruction: NadelResultInstruction.Remove,
    ) {
        @Suppress("UNCHECKED_CAST")
        val map = instruction.subject.value as? MutableJsonMap ?: return

        map.remove(instruction.key.value)
    }

    private fun process(
        error: GraphQLError,
        errors: List<JsonMap?>,
    ) {
        val mutableErrors = errors.asMutable()
        mutableErrors.add(error.toSpecification())
    }

    private fun processGraphQLError(
        error: GraphQLError,
        errors: List<GraphQLError>?,
    ) {
        errors?.asMutable()?.add(error)
    }

    private fun getLocatedError(
        instruction: NadelResultMutation.AddErrorAt,
        resultView: NadelResultView,
    ): GraphQLError {
        val path = resultView
            .getResultPath(instruction.subject)
            ?.toRawPath()
            ?.plus(instruction.relativePath)
        return toGraphQLError(
            raw = instruction.rawError,
            path = path,
        )
    }

    private fun getRemoveArtificialFieldInstructions(
        artificialFields: List<ExecutableNormalizedField>,
        nodes: JsonNodes,
    ): List<NadelResultInstruction> {
        return artificialFields
            .asSequence()
            .flatMap { field ->
                nodes
                    .getNodesAt(
                        queryPath = field.queryPath.dropLast(1),
                        flatten = true,
                    )
                    .map { parentNode ->
                        NadelResultInstruction.Remove(
                            subject = parentNode,
                            key = NadelResultKey(field.resultKey),
                        )
                    }
            }
            .toList()
    }
}

internal fun <K, V> Map<K, V>.asMutable(): MutableMap<K, V> {
    return this as? MutableMap<K, V> ?: throw NotMutableError()
}

private fun <T> List<T>.asMutable(): MutableList<T> {
    return this as? MutableList<T> ?: throw NotMutableError()
}

private class NotMutableError : RuntimeException("Data was required to be mutable but was not")
