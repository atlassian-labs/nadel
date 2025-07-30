package graphql.nadel.engine.transform.result

import graphql.GraphQLError
import graphql.incremental.DeferPayload
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.plan.NadelExecutionPlan
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.MutableJsonMap
import graphql.nadel.engine.util.queryPath
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
        val nodes = JsonNodes(result.data)
        val instructions = getMutationInstructions(
            executionContext,
            serviceExecutionContext,
            executionPlan,
            artificialFields,
            overallToUnderlyingFields,
            service,
            result,
            nodes
        )
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
        result: ServiceExecutionResult,
        deferPayload: DeferPayload,
    ): DeferPayload {
        val nodes = JsonNodes(
            deferPayload.getData<JsonMap?>() ?: emptyMap(),
            pathPrefix = NadelQueryPath(deferPayload.path.filterIsInstance<String>())
        )
        val instructions = getMutationInstructions(
            executionContext,
            serviceExecutionContext,
            executionPlan,
            artificialFields,
            overallToUnderlyingFields,
            service,
            result,
            nodes
        )
        mutate(deferPayload, instructions)
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
        nodes: JsonNodes,
    ): List<NadelResultInstruction> {
        val asyncInstructions = ArrayList<Deferred<List<NadelResultInstruction>>>()
        val contextByTransform = executionPlan.transformContexts
        coroutineScope {
            executionContext.timer.batch { timer ->
                for ((field, steps) in executionPlan.transformationSteps) {
                    val underlyingFields = overallToUnderlyingFields[field]
                    if (underlyingFields.isNullOrEmpty()) continue

                    for (step in steps) {
                        val transformServiceExecutionContext = contextByTransform[step.transform]
                        asyncInstructions.add(
                            async {
                                timer.time(step.resultTransformTimingStep) {
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
                                        transformServiceExecutionContext
                                    )
                                }
                            }
                        )
                    }
                }
            }

            asyncInstructions.add(
                async {
                    getRemoveArtificialFieldInstructions(artificialFields, nodes)
                }
            )
        }
        val instructions = asyncInstructions.awaitAll().flatten()

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
        return instructions
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

    private fun mutate(result: DeferPayload, instructions: List<NadelResultInstruction>) {
        instructions.forEach { transformation ->
            when (transformation) {
                is NadelResultInstruction.Set -> process(transformation)
                is NadelResultInstruction.Remove -> process(transformation)
                is NadelResultInstruction.AddError -> processGraphQLErrors(transformation, result.errors)
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
        errors: List<JsonMap?>,
    ) {
        val newError = instruction.error.toSpecification()

        val mutableErrors = errors.asMutable()
        mutableErrors.add(newError)
    }

    private fun processGraphQLErrors(
        instruction: NadelResultInstruction.AddError,
        errors: List<GraphQLError>?,
    ) {
        errors?.asMutable()?.add(instruction.error)
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
