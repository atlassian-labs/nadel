package graphql.nadel.engine.transform.result.json

import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.util.AnyList
import graphql.nadel.engine.util.AnyMap
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.result.NadelResultPath
import java.util.Collections
import java.util.IdentityHashMap

/**
 * A scoped view over one result payload.
 *
 * Selection delegates to [JsonNodes] and keeps its low-allocation caching behavior. A selected
 * [NadelResultOccurrence] can be located later, after rename and other structural transforms
 * have produced the final client-shaped payload.
 */
internal class NadelResultView private constructor(
    private val data: JsonMap,
    val scope: NadelResultScope,
    private val valueNodes: JsonNodes,
) {
    private val resultPathIndex: ResultPathIndex? by lazy(LazyThreadSafetyMode.NONE) {
        scope.responsePrefix?.let(::makeResultPathIndex)
    }

    /**
     * Compatibility facade for transforms that only require JSON values.
     */
    val nodes: JsonNodes
        get() = valueNodes

    fun getNodesAt(
        queryPath: NadelQueryPath,
        flatten: Boolean = false,
    ): List<JsonNode> {
        return valueNodes.getNodesAt(queryPath, flatten)
    }

    fun getNodeOccurrencesAt(
        queryPath: NadelQueryPath,
        flatten: Boolean = false,
    ): List<NadelResultOccurrence> {
        return getNodesAt(queryPath, flatten).map { node ->
            NadelResultOccurrence(node)
        }
    }

    /**
     * Locates [occurrence] in the current payload shape using response-object identity.
     *
     * This is deliberately evaluated after result mutations. A service alias that has since
     * been renamed or moved therefore cannot leak into a client error path.
     */
    fun getResultPath(
        occurrence: NadelResultOccurrence,
    ): NadelResultPath? {
        val target = occurrence.node.value
        if (target !is AnyMap && target !is AnyList) {
            return null
        }
        val index = resultPathIndex
            ?: return null
        return index.pathsByValue[target]
            ?.takeUnless {
                target in index.ambiguousValues
            }
    }

    /**
     * Indexes the final payload once using reference identity.
     *
     * Reusing one map/list instance at multiple response paths is ambiguous. Those values are
     * retained in [ResultPathIndex.ambiguousValues] so callers receive no path instead of a
     * confidently wrong one.
     */
    private fun makeResultPathIndex(
        responsePrefix: NadelResultPath,
    ): ResultPathIndex {
        val pathsByValue = IdentityHashMap<Any, NadelResultPath>()
        val ambiguousValues = Collections.newSetFromMap(
            IdentityHashMap<Any, Boolean>(),
        )

        fun index(value: Any?, path: NadelResultPath) {
            if (value !is AnyMap && value !is AnyList) {
                return
            }
            if (pathsByValue.containsKey(value)) {
                ambiguousValues.add(value)
                return
            }
            pathsByValue[value] = path

            when (value) {
                is AnyMap -> value.forEach { (key, child) ->
                    val resultKey = key as? String
                        ?: return@forEach
                    index(child, path + resultKey)
                }
                is AnyList -> value.forEachIndexed { childIndex, child ->
                    index(child, path + childIndex)
                }
            }
        }

        index(data, responsePrefix)
        return ResultPathIndex(
            pathsByValue = pathsByValue,
            ambiguousValues = ambiguousValues,
        )
    }

    private data class ResultPathIndex(
        val pathsByValue: IdentityHashMap<Any, NadelResultPath>,
        val ambiguousValues: Set<Any>,
    )

    companion object {
        fun root(data: JsonMap): NadelResultView {
            return create(
                data = data,
                scope = NadelResultScope.root,
            )
        }

        fun deferred(
            data: JsonMap,
            path: List<Any>,
        ): NadelResultView {
            return create(
                data = data,
                scope = NadelResultScope.deferred(path),
            )
        }

        fun unaddressable(
            data: JsonMap,
            queryPrefix: NadelQueryPath = NadelQueryPath.root,
        ): NadelResultView {
            return create(
                data = data,
                scope = NadelResultScope.unaddressable(queryPrefix),
            )
        }

        fun scoped(
            data: JsonMap,
            scope: NadelResultScope,
        ): NadelResultView {
            return create(
                data = data,
                scope = scope,
            )
        }

        internal fun create(
            data: JsonMap,
            scope: NadelResultScope,
        ): NadelResultView {
            val pathPrefix = scope.queryPrefix.takeUnless { queryPrefix ->
                queryPrefix == NadelQueryPath.root
            }
            return NadelResultView(
                data = data,
                scope = scope,
                valueNodes = JsonNodes(
                    data = data,
                    pathPrefix = pathPrefix,
                ),
            )
        }
    }
}
