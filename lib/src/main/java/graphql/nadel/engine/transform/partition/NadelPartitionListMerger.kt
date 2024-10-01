package graphql.nadel.engine.transform.partition

import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.util.unwrapNonNull
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLList
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLScalarType

object NadelPartitionListMerger {
    fun mergeDataFromList(
        dataFromPartitionCalls: List<Any?>,
        thisNodesData: Any?,
        parentNodes: List<JsonNode>,
        overallField: ExecutableNormalizedField,
    ): List<NadelResultInstruction.Set> {
        val parentNode = parentNodes.first()

        val listDataFromPartitionCalls = dataFromPartitionCalls
            .mapNotNull {
                if (it == null) {
                    null
                } else {
                    check(it is List<*>) { "Expected a list, but got ${it::class.simpleName}" }
                    it
                }
            }.flatten()

        val thisNodesDataCast = thisNodesData?.let {
            check(it is List<*>) { "Expected a list, but got ${it::class.simpleName}" }
            it
        } ?: emptyList<NadelResultInstruction.Set>()

        return listOf(
            NadelResultInstruction.Set(
                subject = parentNode,
                newValue = JsonNode(thisNodesDataCast + listDataFromPartitionCalls),
                field = overallField
            )
        )
    }

}
