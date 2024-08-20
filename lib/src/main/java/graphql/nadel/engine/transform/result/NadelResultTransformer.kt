package graphql.nadel.engine.transform.result

import graphql.incremental.DeferPayload
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.incremental.DelayedIncrementalPartialResultImpl
import graphql.nadel.NadelIncrementalServiceExecutionResult
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.plan.NadelExecutionPlan
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.transform.result.json.NadelCachingJsonNodes
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.MutableJsonMap
import graphql.nadel.engine.util.queryPath
import graphql.normalized.ExecutableNormalizedField
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow

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
        if (result is NadelIncrementalServiceExecutionResult) {
            result.incrementalItemPublisher.asFlow()
                .map {
                    val incremental = it.incremental
                        ?.map {

                        }
                    DelayedIncrementalPartialResultImpl.newIncrementalExecutionResult()

                        .extensions(it.extensions)
                        .build()
                }
        }

        val nodes = JsonNodes(result.data)

        val asyncInstructions = ArrayList<Deferred<List<NadelResultInstruction>>>()

        coroutineScope {
            for ((field, steps) in executionPlan.transformationSteps) {
                // This can be null if we did not end up sending the field e.g. for hydration
                val underlyingFields = overallToUnderlyingFields[field]
                if (underlyingFields.isNullOrEmpty()) {
                    continue
                }

                for (step in steps) {
                    asyncInstructions.add(
                        async {
                            step.transform.getResultInstructions(
                                executionContext,
                                serviceExecutionContext,
                                executionBlueprint,
                                service,
                                field,
                                underlyingFields.first().parent,
                                result,
                                step.state,
                                nodes,
                            )
                        },
                    )
                }
            }

            asyncInstructions.add(
                async {
                    getRemoveArtificialFieldInstructions(artificialFields, nodes)
                },
            )
        }

        val instructions = asyncInstructions
            .awaitAll()
            .flatten()

        mutate(result, instructions)

        return result
    }

    suspend fun transform(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionPlan: NadelExecutionPlan,
        artificialFields: List<ExecutableNormalizedField>,
        overallToUnderlyingFields: Map<ExecutableNormalizedField, List<ExecutableNormalizedField>>,
        service: Service,
        result: NadelIncrementalServiceExecutionResult,
        delayedIncrementalPartialResult: DelayedIncrementalPartialResult // NadelIncrementalServiceExecutionResult,
    ): DelayedIncrementalPartialResult {
        val asyncInstructions = ArrayList<Deferred<List<NadelResultInstruction>>>()

        coroutineScope {
            delayedIncrementalPartialResult.incremental
                ?.filterIsInstance<DeferPayload>() // We need this filter because IncrementalPayloads could be stream or defer
                ?.map { deferPayload ->
                    val nodes = NadelCachingJsonNodes(
                        deferPayload.getData<JsonMap?>() ?: emptyMap(),
                        pathPrefix = deferPayload.path.filterIsInstance<String>(), //converts resultPath to queryPath TODO: is it better for this to be NadelQueryPath? as that gives us better type safety
                    )

                    for ((field, steps) in executionPlan.transformationSteps) {
                        // This can be null if we did not end up sending the field e.g. for hydration
                        val underlyingFields = overallToUnderlyingFields[field]
                        if (underlyingFields.isNullOrEmpty()) {
                            continue
                        }

                        for (step in steps) {
                            asyncInstructions.add(
                                async {
                                    step.transform.getResultInstructions(
                                        executionContext,
                                        serviceExecutionContext,
                                        executionBlueprint,
                                        service,
                                        field,
                                        underlyingFields.first().parent,
                                        result,
                                        step.state,
                                        nodes,
                                    )
                                },
                            )
                        }

                        asyncInstructions.add(
                            async {
                                getRemoveArtificialFieldInstructions(artificialFields, nodes)
                            },
                        )
                    }
                }
        }
        val instructions = asyncInstructions
            .awaitAll()
            .flatten()

        mutate(delayedIncrementalPartialResult, instructions)

        return delayedIncrementalPartialResult
    }

    private fun mutate(result: ServiceExecutionResult, instructions: List<NadelResultInstruction>) {
        instructions.forEach { transformation ->
            when (transformation) {
                is NadelResultInstruction.Set -> process(transformation)
                is NadelResultInstruction.Remove -> process(transformation)
                is NadelResultInstruction.AddError -> process(transformation, result.errors)
            }
        }
    }

    private fun mutate(result: DelayedIncrementalPartialResult, instructions: List<NadelResultInstruction>) {
        instructions.forEach { transformation ->
            when (transformation) {
                is NadelResultInstruction.Set -> process(transformation)
                is NadelResultInstruction.Remove -> process(transformation)
                is NadelResultInstruction.AddError -> process(transformation, emptyList()) // result.incremental?.first()?.errors)   TODO: add errors properly on this line
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
        instruction: NadelResultInstruction.AddError,
        errors: List<JsonMap>,
    ) {
        val newError = instruction.error.toSpecification()

        val mutableErrors = errors.asMutable()
        mutableErrors.add(newError)
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
