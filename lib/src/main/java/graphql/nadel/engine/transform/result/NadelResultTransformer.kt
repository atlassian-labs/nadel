package graphql.nadel.engine.transform.result

import graphql.GraphQLError
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.plan.NadelExecutionPlan
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.MutableJsonMap
import graphql.nadel.engine.util.queryPath
import graphql.normalized.ExecutableNormalizedField
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal class NadelResultTransformer(
    private val executionBlueprint: NadelOverallExecutionBlueprint,
) {
    internal data class MutateResult(
        val errorsAdded: List<GraphQLError>,
    )

    suspend fun mutate(
        executionContext: NadelExecutionContext,
        executionPlan: NadelExecutionPlan,
        artificialFields: List<ExecutableNormalizedField>,
        overallToUnderlyingFields: Map<ExecutableNormalizedField, List<ExecutableNormalizedField>>,
        service: Service,
        result: ServiceExecutionResult,
    ): MutateResult {
        val nodes = JsonNodes(result.data)

        val deferredInstructions = ArrayList<Deferred<List<NadelResultInstruction>>>()

        coroutineScope {
            for ((field, steps) in executionPlan.transformationSteps) {
                // This can be null if we did not end up sending the field e.g. for hydration
                val underlyingFields = overallToUnderlyingFields[field]
                if (underlyingFields.isNullOrEmpty()) {
                    continue
                }

                for (step in steps) {
                    deferredInstructions.add(
                        async {
                            step.transform.getResultInstructions(
                                executionContext,
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

            deferredInstructions.add(
                async {
                    getRemoveArtificialFieldInstructions(artificialFields, nodes)
                },
            )
        }

        val instructions = deferredInstructions
            .awaitAll()
            .flatten() // todo: why do we even flatten here? we can just iterate

        mutate(result, instructions)

        val errors = instructions
            .mapNotNull {
                (it as? NadelResultInstruction.AddError)?.error
            }

        return MutateResult(errors)
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
                nodes.getNodesAt(
                    queryPath = field.queryPath.dropLast(1),
                    flatten = true,
                ).map { parentNode ->
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
