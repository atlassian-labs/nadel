package graphql.nadel.engine.transform.partition

import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.util.unwrapNonNull
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLList
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLScalarType

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
