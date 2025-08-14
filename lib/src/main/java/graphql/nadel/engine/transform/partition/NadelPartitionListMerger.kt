package graphql.nadel.engine.transform.partition

import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.normalized.ExecutableNormalizedField

internal object NadelPartitionListMerger {
    fun mergeDataFromList(
        dataFromPartitionCalls: List<Any?>,
        thisNodesData: Any?,
        parentNode: JsonNode,
        overallField: ExecutableNormalizedField,
    ): List<NadelResultInstruction.Set> {
        val listDataFromPartitionCalls = dataFromPartitionCalls
            .flatMap {
                if (it == null) {
                    emptyList()
                } else {
                    it as List<*>
                }
            }

        val thisNodesDataCast = thisNodesData?.let {
            it as List<*>
        } ?: emptyList<Any?>()

        return listOf(
            NadelResultInstruction.Set(
                subject = parentNode,
                newValue = JsonNode(thisNodesDataCast + listDataFromPartitionCalls),
                field = overallField
            )
        )
    }

}
