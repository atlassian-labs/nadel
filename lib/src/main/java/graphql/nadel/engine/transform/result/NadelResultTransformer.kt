package graphql.nadel.engine.transform.result

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.plan.NadelExecutionPlan
import graphql.nadel.engine.transform.result.NadelResultTransformer.DataMutation
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodePath
import graphql.nadel.engine.transform.result.json.JsonNodePathSegment
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.AnyList
import graphql.nadel.engine.util.AnyMap
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.MutableJsonMap
import graphql.nadel.engine.util.queryPath
import graphql.normalized.ExecutableNormalizedField
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.reflect.KClass

internal class NadelResultTransformer(private val executionBlueprint: NadelOverallExecutionBlueprint) {
    suspend fun transform(
        executionContext: NadelExecutionContext,
        executionPlan: NadelExecutionPlan,
        artificialFields: List<ExecutableNormalizedField>,
        overallToUnderlyingFields: Map<ExecutableNormalizedField, List<ExecutableNormalizedField>>,
        service: Service,
        result: ServiceExecutionResult,
    ): ServiceExecutionResult {
        val nodes = JsonNodes(result.data, executionContext.hints)

        val deferredInstructions = ArrayList<Deferred<List<NadelResultInstruction>>>()

        coroutineScope {
            for ((field, steps) in executionPlan.transformationSteps) {
                // This can be null if we did not end up sending the field e.g. for hydration
                val underlyingFields = overallToUnderlyingFields[field]
                if (underlyingFields == null || underlyingFields.isEmpty()) {
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
            .flatten()

        mutate(result, instructions)

        return result
    }

    private fun mutate(result: ServiceExecutionResult, instructions: List<NadelResultInstruction>) {
        // For now, we don't have any instructions to modify anything other than data, so return early
        if (result.data == null) {
            return
        }

        val mutations = instructions.mapNotNull { transformation ->
            when (transformation) {
                is NadelResultInstruction.Set -> prepareSet(result.data, transformation)
                is NadelResultInstruction.Remove -> prepareRemove(result.data, transformation)
                is NadelResultInstruction.AddError -> prepareAddError(result.errors, transformation)
            }
        }

        val transformContext = TransformContext()
        mutations.forEach {
            it.run(context = transformContext)
        }
    }

    private fun prepareSet(
        data: JsonMap,
        instruction: NadelResultInstruction.Set,
    ): DataMutation? {
        @Suppress("UNCHECKED_CAST")
        val map = instruction.subject.value as? MutableJsonMap ?: return null

        return DataMutation {
            map[instruction.key.value] = instruction.newValue
        }
    }

    private fun prepareRemove(
        data: JsonMap,
        instruction: NadelResultInstruction.Remove,
    ): DataMutation? {
        @Suppress("UNCHECKED_CAST")
        val map = instruction.subject.value as? MutableJsonMap ?: return null

        return DataMutation {
            map.remove(instruction.key.value)
        }
    }

    private fun prepareAddError(
        errors: List<JsonMap>,
        instruction: NadelResultInstruction.AddError,
    ): DataMutation {
        val newError = instruction.error.toSpecification()

        return DataMutation {
            val mutableErrors = errors.asMutable()
            mutableErrors.add(newError)
        }
    }

    private inline fun modifyMap(
        context: TransformContext,
        mapPath: JsonNodePath,
        map: JsonMap,
        modification: (MutableJsonMap) -> Unit,
    ) {
        val wasEmpty = map.isEmpty()
        modification(map.asMutable())

        if (map.isEmpty()) {
            if (!wasEmpty) {
                context.emptyPaths.add(mapPath)
            }
        } else if (wasEmpty) {
            context.emptyPaths.remove(mapPath)
        }
    }

    private fun interface DataMutation {
        fun run(context: TransformContext)
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
                        key = ResultKey(field.resultKey),
                    )
                }
            }
            .toList()
    }

    private data class TransformContext(
        val emptyPaths: HashSet<JsonNodePath> = hashSetOf(),
    )
}

internal fun <K, V> Map<K, V>.asMutable(): MutableMap<K, V> {
    return this as? MutableMap<K, V> ?: throw NotMutableError()
}

private fun <T> List<T>.asMutable(): MutableList<T> {
    return this as? MutableList<T> ?: throw NotMutableError()
}

/**
 * This gets the object right before the last segment of the [node], aka the parent.
 */
private fun JsonMap.getParentOf(node: JsonNodePath): JsonMap? {
    val pathToParent = node.dropLast(1)
    return getJsonMapAt(pathToParent)
}

private fun JsonMap.getJsonMapAt(path: JsonNodePath): JsonMap? {
    @Suppress("UNCHECKED_CAST")
    return getAt(path).value as JsonMap?
}

private fun JsonMap.getAt(path: JsonNodePath): JsonNode {
    var cursor: Any = this

    for (segment in path.segments) {
        cursor = when (segment) {
            is JsonNodePathSegment.Int -> {
                if (cursor is AnyList) {
                    cursor[segment.value]
                } else {
                    error("Requires List")
                }
            }

            is JsonNodePathSegment.String -> {
                if (cursor is AnyMap) {
                    cursor[segment.value]
                } else {
                    error("Requires List")
                }
            }
        } ?: return JsonNode(null)
    }

    return JsonNode(
        cursor,
    )
    // return path.segments.fold(JsonNode(JsonNodePath.root, this), JsonNode::get)
}

private class NotMutableError : RuntimeException("Data was required to be mutable but was not")

private class UnexpectedDataType private constructor(message: String) : RuntimeException(message) {
    companion object {
        operator fun invoke(path: JsonNodePath, expected: KClass<*>, actual: Class<*>?): UnexpectedDataType {
            val pathStr = path.segments.joinToString("/") {
                it.value.toString()
            }
            val expectedStr = expected.qualifiedName
            val actualStr = actual?.name ?: "null"
            val message = "Expected data at '$pathStr' to be of type '$expectedStr' but got '$actualStr'"
            return UnexpectedDataType(message)
        }
    }
}
